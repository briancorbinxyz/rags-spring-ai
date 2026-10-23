#!/usr/bin/env bash
# Build llama-server with CUDA on Ubuntu, under WSL or on the metal, and install what the ratchet
# runner needs around it. Idempotent: run it again after fixing whatever it stopped on.
#
#   tools/ratchet-proto/setup_llama_cuda.sh --dry-run    # say what it would do
#   tools/ratchet-proto/setup_llama_cuda.sh
#
# Only two things really differ between WSL and a native Ubuntu box, and both are about the driver:
#
#   WSL     the NVIDIA driver belongs to WINDOWS. Never install a driver inside the distro — it
#           breaks the /usr/lib/wsl passthrough. You install only the CUDA toolkit, from NVIDIA's
#           separate wsl-ubuntu repo, which deliberately ships no kernel driver.
#   native  the driver and the toolkit both live here, from the ubuntuXXXX repo.
#
# Everything after that — libcurl, cmake, the build flags, the binary — is identical.
set -euo pipefail

DRY=0; YES=0; LLAMA_DIR="${LLAMA_DIR:-$HOME/llama.cpp}"; JOBS="${JOBS:-$(nproc 2>/dev/null || echo 4)}"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY=1; shift ;;
    --yes|-y)  YES=1; shift ;;
    --dir)     LLAMA_DIR="$2"; shift 2 ;;
    -h|--help) awk 'NR>1 && /^#/ {sub(/^# ?/, ""); print; next} NR>1 {exit}' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

# The login shell decides which rc file a PATH line belongs in; a zsh user never reads .bashrc.
case "$(basename "${SHELL:-bash}")" in
  zsh)  RC="$HOME/.zshrc" ;;
  bash) RC="$HOME/.bashrc" ;;
  fish) RC="" ;;                       # fish syntax differs; tell, do not write
  *)    RC="$HOME/.profile" ;;
esac

say()  { printf '\n\033[1m== %s\033[0m\n' "$*"; }
note() { printf '   %s\n' "$*"; }
die()  { printf '\n\033[31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }
run()  { if (( DRY )); then printf '   $ %s\n' "$*"; else printf '   $ %s\n' "$*"; eval "$@"; fi; }

# --- which kind of machine is this ---------------------------------------------------------------
if grep -qiE 'microsoft|wsl' /proc/version 2>/dev/null || [[ -d /usr/lib/wsl ]]; then
  KIND=wsl
else
  KIND=native
fi
. /etc/os-release 2>/dev/null || true
CODE="${VERSION_ID:-}"; DISTRO_ID="ubuntu${CODE//./}"
say "machine: $KIND ($PRETTY_NAME)"
[[ "$KIND" == wsl ]] && note "the driver is Windows' — this script will not install one here"

# --- the GPU, as the distro sees it ---------------------------------------------------------------
say "GPU"
if command -v nvidia-smi >/dev/null && nvidia-smi -L >/dev/null 2>&1; then
  note "$(nvidia-smi --query-gpu=name,memory.total,driver_version --format=csv,noheader | head -1)"
else
  if [[ "$KIND" == wsl ]]; then
    die "nvidia-smi does not work in this distro.
  The driver is installed on WINDOWS, not here. In PowerShell check 'nvidia-smi' works, update the
  GeForce/Studio driver if not, then restart WSL:  wsl --shutdown
  Do NOT 'apt install nvidia-driver-*' inside WSL; it breaks the passthrough in /usr/lib/wsl."
  else
    die "no working nvidia-smi. On a native Ubuntu box install the driver first:
    sudo ubuntu-drivers install          # or: sudo apt install nvidia-driver-560
    sudo reboot"
  fi
fi

# --- build dependencies, and the two python packages the runner needs -----------------------------
say "build dependencies"
PKGS=(build-essential cmake git libcurl4-openssl-dev pkg-config)
PYVER="$(python3 -c 'import sys; print(f"{sys.version_info.major}.{sys.version_info.minor}")' 2>/dev/null || echo 3)"
PKGS+=("python${PYVER}-venv" python3-pip-whl)       # venv and the wheels ensurepip needs
MISSING=()
for p in "${PKGS[@]}"; do dpkg -s "$p" >/dev/null 2>&1 || MISSING+=("$p"); done
if (( ${#MISSING[@]} )); then
  note "missing: ${MISSING[*]}"
  (( YES || DRY )) || { read -rp "   install them with apt? [Y/n] " a; [[ "${a:-y}" =~ ^[Yy]?$ ]] || die "stopped"; }
  run "sudo apt-get update"
  run "sudo apt-get install -y ${MISSING[*]}"
else
  note "all present"
fi

# --- the CUDA toolkit (nvcc). Guided, not installed: it adds a third-party apt repo ----------------
say "CUDA toolkit"
if command -v nvcc >/dev/null; then
  note "$(nvcc --version | tail -2 | head -1)"
elif [[ -x /usr/local/cuda/bin/nvcc ]]; then
  note "found at /usr/local/cuda/bin/nvcc, but not on PATH"
  note "add it:  echo 'export PATH=/usr/local/cuda/bin:\$PATH' >> ${RC:-~/.profile} && source ${RC:-~/.profile}"
  export PATH=/usr/local/cuda/bin:$PATH
else
  REPO=$([[ "$KIND" == wsl ]] && echo "wsl-ubuntu" || echo "$DISTRO_ID")
  die "nvcc not found, and llama.cpp cannot be built with CUDA without it.
  This adds an NVIDIA apt repo, so it is left to you rather than done silently:

    wget https://developer.download.nvidia.com/compute/cuda/repos/${REPO}/x86_64/cuda-keyring_1.1-1_all.deb
    sudo dpkg -i cuda-keyring_1.1-1_all.deb
    sudo apt-get update
    sudo apt-get install -y cuda-toolkit          # NOT 'cuda', which pulls a driver
    echo 'export PATH=/usr/local/cuda/bin:\$PATH' >> ${RC:-~/.profile} && source ${RC:-~/.profile}

  The repo above is the ${KIND} one$([[ "$KIND" == wsl ]] && echo ", which ships no kernel driver — that is the point" || echo ""). Then run this script again."
fi

# --- can this toolkit emit code for this card? ----------------------------------------------------
# nvidia-smi's "CUDA Version" is the driver's ceiling and says nothing about which architectures
# nvcc can target. Blackwell (RTX 50-series, sm_120) needs CUDA 12.8 or newer; an older toolkit
# builds something that will not run on the card, and says so only at launch.
if command -v nvcc >/dev/null && command -v nvidia-smi >/dev/null; then
  CAP="$(nvidia-smi --query-gpu=compute_cap --format=csv,noheader 2>/dev/null | head -1 | tr -d ' ')"
  NVCC_V="$(nvcc --version | sed -n 's/.*release \([0-9][0-9]*\.[0-9][0-9]*\).*/\1/p' | head -1)"
  if [[ -n "$CAP" && -n "$NVCC_V" ]]; then
    note "GPU compute capability $CAP, toolkit $NVCC_V"
    cap_n=${CAP%%.*}; nv_maj=${NVCC_V%%.*}; nv_min=${NVCC_V##*.}
    if (( cap_n >= 12 )) && { (( nv_maj < 12 )) || { (( nv_maj == 12 )) && (( nv_min < 8 )); }; }; then
      die "this GPU is compute capability $CAP (Blackwell), but the toolkit is CUDA $NVCC_V.
  sm_120 needs CUDA 12.8 or newer. Install a current toolkit before building, or the binary will
  compile and then fail on the card:
    sudo apt-get install -y cuda-toolkit"
    fi
  fi
fi

# --- llama.cpp ------------------------------------------------------------------------------------
say "llama.cpp"
if [[ -d "$LLAMA_DIR/.git" ]]; then
  note "updating $LLAMA_DIR"
  run "git -C '$LLAMA_DIR' pull --ff-only"
else
  run "git clone https://github.com/ggml-org/llama.cpp '$LLAMA_DIR'"
fi

# cmake caches a failed find_package(CURL), so a stale build dir keeps a curl-less binary forever
if [[ -f "$LLAMA_DIR/build/CMakeCache.txt" ]] && grep -q 'CURL_LIBRARY:FILEPATH=CURL_LIBRARY-NOTFOUND' "$LLAMA_DIR/build/CMakeCache.txt" 2>/dev/null; then
  note "the existing build dir remembers CURL-NOTFOUND; removing it so the lookup runs again"
  run "rm -rf '$LLAMA_DIR/build'"
fi

say "configure and build (this takes a while)"
run "cmake -S '$LLAMA_DIR' -B '$LLAMA_DIR/build' -DGGML_CUDA=ON -DLLAMA_CURL=ON -DCMAKE_BUILD_TYPE=Release"
run "cmake --build '$LLAMA_DIR/build' --config Release -j${JOBS}"

# --- verify, and say so in terms of what the runner needs -----------------------------------------
say "verify"
BIN="$LLAMA_DIR/build/bin/llama-server"
if (( DRY )); then note "would check $BIN for CUDA and libcurl"; else
  [[ -x "$BIN" ]] || die "no llama-server at $BIN — the build did not produce one"
  ldd "$BIN" | grep -qi libcurl \
    && note "libcurl: linked (so -hf can fetch models)" \
    || die "built, but without libcurl: -hf will not work.
  sudo apt install -y libcurl4-openssl-dev && rm -rf '$LLAMA_DIR/build' && run this again"
  ldd "$BIN" | grep -qiE 'libcudart|libcuda' \
    && note "CUDA: linked" \
    || note "WARNING: no CUDA in the linkage; -ngl 99 will fall back to CPU"
  note "$("$BIN" --version 2>&1 | head -1)"
fi

# --- PATH -----------------------------------------------------------------------------------------
say "PATH"
if command -v llama-server >/dev/null && [[ "$(command -v llama-server)" == "$BIN" ]]; then
  note "llama-server already resolves to the build"
else
  LINE="export PATH=\"$LLAMA_DIR/build/bin:\$PATH\""
  if [[ -z "$RC" ]]; then
    note "your shell is $(basename "$SHELL"); add this yourself, in its own syntax:"
    note "  set -gx PATH $LLAMA_DIR/build/bin \$PATH      # fish"
  elif grep -qF "$LLAMA_DIR/build/bin" "$RC" 2>/dev/null; then
    note "$RC already has it; open a new shell (or: source $RC)"
  else
    run "echo '$LINE' >> '$RC'"
    note "added to $RC; open a new shell (or: source $RC)"
  fi
fi

say "done"
note "next:  tools/ratchet-proto/run_5090_wsl.sh --dry-run"
