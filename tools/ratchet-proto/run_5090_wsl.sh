#!/usr/bin/env bash
# The ratchet experiment on an RTX 5090, from WSL (Ubuntu). The Linux twin of run_5090.ps1.
#
# For each run in the lineup it starts llama-server for the embedder on 8090 and the reranker on
# 8091 (models fetched with -hf into the Hugging Face cache on first use), waits for /health, runs
# run.py, and stops both servers even on error. It never touches another llama-server, and a run
# whose .md and .jsonl both exist is skipped unless --force.
#
# First time, from a WSL Ubuntu shell:
#
#   git clone -b episode-4-prototype https://github.com/briancorbinxyz/rags-spring-ai.git
#   cd rags-spring-ai
#   tools/ratchet-proto/run_5090_wsl.sh --dry-run        # check the plan, download nothing
#
# spectastic is cloned for you, pinned, as a sibling of the repo.
#
#   tools/ratchet-proto/run_5090_wsl.sh --list
#   tools/ratchet-proto/run_5090_wsl.sh --dry-run      # what it would start and run, downloading nothing
#   tools/ratchet-proto/run_5090_wsl.sh                       # the default lineup
#   tools/ratchet-proto/run_5090_wsl.sh --models qwen3-emb-8b,qwen3-emb-8b-qwen3-rr
#   tools/ratchet-proto/run_5090_wsl.sh --models qwen3-emb-8b -- --only S1,S2,S3,S4,S5,X1,H1,E5-1
#
# Everything after a bare -- is passed through to run.py.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
RESULTS="$HERE/results"
PINNED=c64d41c2e5d0d42c4fefd1b506cb083b9812bf6c
SPECTASTIC="${SPECTASTIC:-$REPO/../spectastic}"
EMBED_PORT="${EMBED_PORT:-8090}"
RERANK_PORT="${RERANK_PORT:-8091}"
START_TIMEOUT="${START_TIMEOUT:-3600}"          # seconds per server, first download included
MODELS=""; FORCE=0; LIST=0; DRY=0; EXTRA=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --models) MODELS="$2"; shift 2 ;;
    --force)  FORCE=1; shift ;;
    --list)   LIST=1; shift ;;
    --dry-run) DRY=1; shift ;;
    --)       shift; EXTRA=("$@"); break ;;
    -h|--help) sed -n '2,20p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

mkdir -p "$RESULTS"
LOG="$RESULTS/5090-run.log"
log() { printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" | tee -a "$LOG"; }
die() { log "ERROR: $*"; exit 1; }

# --- preflight ---------------------------------------------------------------------------------
command -v git     >/dev/null || die "git is not installed (sudo apt install git)"
command -v curl    >/dev/null || die "curl is not installed (sudo apt install curl)"
command -v python3 >/dev/null || die "python3 is not installed (sudo apt install python3 python3-venv)"
python3 -c 'import sys; sys.exit(0 if sys.version_info >= (3,11) else 1)' \
  || die "python3 is $(python3 -V), but 3.11+ is needed"
python3 -c 'import sqlite3; sqlite3.connect(":memory:").execute("create virtual table t using fts5(x)")' \
  2>/dev/null || die "this python's sqlite has no FTS5 (sudo apt install python3 libsqlite3-dev)"

# Debian and Ubuntu ship venv and pip as separate packages, so a bare WSL Ubuntu has neither.
# Find that out here, with the version-correct package name, rather than part way through.
PYVER="$(python3 -c 'import sys; print(f"{sys.version_info.major}.{sys.version_info.minor}")')"
python3 -c 'import ensurepip' 2>/dev/null \
  || die "python3 has no ensurepip, so 'python3 -m venv' cannot work. On Ubuntu:
    sudo apt update && sudo apt install -y python${PYVER}-venv
  (python3-venv also works; the versioned name is the one apt will actually name in its error.)"

# --- the venv: numpy and tiktoken only (the reranker runs in llama-server) -----------------------
VENV="$HERE/.venv"
if [[ ! -x "$VENV/bin/python" ]]; then
  # A half-made venv from an earlier failure has no bin/python and would never be retried.
  [[ -d "$VENV" ]] && { log "removing an incomplete venv at $VENV"; rm -rf "$VENV"; }
  log "creating venv in $VENV"
  if ! python3 -m venv "$VENV"; then
    die "could not create the venv. On Ubuntu this is nearly always the missing package:
    sudo apt update && sudo apt install -y python${PYVER}-venv
  then run this script again."
  fi
fi
PY="$VENV/bin/python"
[[ -x "$PY" ]] || die "the venv exists but has no python at $PY. Delete it and run again: rm -rf '$VENV'"
"$PY" -m pip --version >/dev/null 2>&1 \
  || die "the venv has no pip. Install the venv package and remake it:
    sudo apt install -y python${PYVER}-venv && rm -rf '$VENV'"
log "venv: $("$PY" -V), pip $("$PY" -m pip --version | awk '{print $2}')"
# Errors are shown, not swallowed: a failure here is the thing you need to read.
"$PY" -m pip install --quiet --upgrade pip || log "WARNING: could not upgrade pip, continuing"
"$PY" -m pip install --quiet numpy tiktoken || die "pip install numpy tiktoken failed (see the output above)"

# --- the lineup, straight from ratchet/llamacpp.py so there is one model table -------------------
PLAN_ARGS=(--embed-port "$EMBED_PORT" --rerank-port "$RERANK_PORT")
[[ -n "$MODELS" ]] && PLAN_ARGS+=(--runs "$MODELS")
if (( LIST )); then "$PY" "$HERE/ratchet/llamacpp.py" list "${PLAN_ARGS[@]}"; exit 0; fi

if (( ! DRY )); then
if ! command -v nvidia-smi >/dev/null; then
  die "nvidia-smi not found inside WSL. Install the Windows NVIDIA driver with WSL support; the
  driver is NOT installed inside Ubuntu. Check with: nvidia-smi.exe in PowerShell, then nvidia-smi here."
fi
log "GPU: $(nvidia-smi --query-gpu=name,memory.total --format=csv,noheader | head -1)"

if ! command -v llama-server >/dev/null; then
  die "llama-server is not on PATH. Build it with CUDA inside WSL:
    sudo apt update && sudo apt install -y build-essential cmake libcurl4-openssl-dev
    # CUDA toolkit for WSL (nvcc), if you do not have it:
    #   https://developer.nvidia.com/cuda-downloads  ->  Linux / x86_64 / WSL-Ubuntu
    git clone https://github.com/ggml-org/llama.cpp ~/llama.cpp
    cmake -S ~/llama.cpp -B ~/llama.cpp/build -DGGML_CUDA=ON -DLLAMA_CURL=ON
    cmake --build ~/llama.cpp/build --config Release -j\$(nproc)
    echo 'export PATH=\$HOME/llama.cpp/build/bin:\$PATH' >> ~/.bashrc && source ~/.bashrc
  -DLLAMA_CURL=ON matters: the lineup fetches models with -hf."
fi
log "llama-server: $(command -v llama-server)"
llama-server --version 2>&1 | head -2 | while read -r l; do log "  $l"; done

# --- the corpus, pinned ------------------------------------------------------------------------
if [[ ! -d "$SPECTASTIC/specs" ]]; then
  log "cloning spectastic into $SPECTASTIC"
  git clone --depth 1 --branch v0.1.0-pre.28 https://github.com/spectastic/spectastic.git "$SPECTASTIC"
fi
AT="$(git -C "$SPECTASTIC" rev-parse HEAD 2>/dev/null || echo unknown)"
[[ "$AT" == "$PINNED" ]] || log "WARNING: spectastic is at $AT, not the pinned $PINNED: the numbers are a new baseline."
fi
PLAN="$("$PY" "$HERE/ratchet/llamacpp.py" plan "${PLAN_ARGS[@]}")"
COUNT="$(printf '%s' "$PLAN" | "$PY" -c 'import json,sys; print(len(json.load(sys.stdin)))')"
log "lineup: $COUNT run(s)"

for (( i = 0; i < COUNT; i++ )); do
  field() { printf '%s' "$PLAN" | "$PY" -c "
import json,sys
r = json.load(sys.stdin)[$i]
p = '$1'.split('.')
v = r
for k in p:
    v = v.get(k) if isinstance(v, dict) else None
    if v is None: break
print('' if v is None else (' '.join(v) if isinstance(v, list) else v))"; }

  NAME="$(field name)"
  OUT="$RESULTS/5090-$NAME"
  if [[ -f "$OUT.md" && -f "$OUT.jsonl" && $FORCE -eq 0 ]]; then
    log "== $NAME: already done, skipping (--force to redo)"; continue
  fi

  PIDS=(); started=()
  stop_all() {
    for pid in "${PIDS[@]:-}"; do
      [[ -n "${pid:-}" ]] && kill "$pid" 2>/dev/null && wait "$pid" 2>/dev/null || true
    done
    PIDS=()
  }
  trap 'stop_all' EXIT INT TERM

  start_server() {   # role, alias, args..., -> sets PIDS
    local role="$1" alias="$2" port="$3"; shift 3
    local slog="$RESULTS/5090-$NAME-$role.server.log"
    log "  starting llama-server for $alias on :$port (log: $(basename "$slog"))"
    llama-server "$@" >"$slog" 2>&1 &
    PIDS+=("$!")
    local waited=0
    until curl -sf "http://127.0.0.1:$port/health" >/dev/null 2>&1; do
      sleep 2; waited=$((waited + 2))
      if ! kill -0 "${PIDS[-1]}" 2>/dev/null; then
        log "  llama-server died; last lines:"; tail -15 "$slog" | sed 's/^/    /' | tee -a "$LOG"
        return 1
      fi
      (( waited >= START_TIMEOUT )) && { log "  timed out after ${waited}s"; return 1; }
    done
    log "  healthy after ${waited}s"
  }

  if (( DRY )); then
    echo "== $NAME — $(field why)"
    echo "   embedder : llama-server $(field embedder.args)"
    [[ -n "$(field reranker.alias)" ]] && echo "   reranker : llama-server $(field reranker.args)"
    echo "   download : $(field embedder.size_gb) GB + $(field reranker.size_gb) GB on first use"
    echo "   then     : run.py --spectastic $SPECTASTIC --embedder llamacpp:$(field embedder.alias) \\"
    echo "                     --llamacpp-url $(field embedder.url) \\"
    [[ -n "$(field reranker.alias)" ]] && echo "                     --reranker llamacpp:$(field reranker.alias) --llamacpp-rerank-url $(field reranker.url) \\"
    echo "                     --out $OUT ${EXTRA[*]:-}"
    echo
    continue
  fi

  log "== $NAME ($(field why))"
  t0=$SECONDS
  ok=1
  start_server embed "$(field embedder.alias)" "$EMBED_PORT" $(field embedder.args) || ok=0
  PY_ARGS=(--spectastic "$SPECTASTIC" --embedder "llamacpp:$(field embedder.alias)"
           --llamacpp-url "$(field embedder.url)" --out "$OUT")
  if (( ok )) && [[ -n "$(field reranker.alias)" ]]; then
    start_server rerank "$(field reranker.alias)" "$RERANK_PORT" $(field reranker.args) || ok=0
    PY_ARGS+=(--reranker "llamacpp:$(field reranker.alias)" --llamacpp-rerank-url "$(field reranker.url)")
  fi
  if (( ok )); then
    "$PY" "$HERE/run.py" "${PY_ARGS[@]}" "${EXTRA[@]:-}" 2>&1 | tee -a "$LOG" || ok=0
  fi
  stop_all
  trap - EXIT INT TERM
  if (( ok )); then
    log "== $NAME: done in $(( (SECONDS - t0) / 60 )) min -> $OUT.md"
  else
    log "== $NAME: FAILED"
  fi
done

log "all done. Results:"
ls -1 "$RESULTS"/5090-*.md 2>/dev/null | tee -a "$LOG" || log "  (none)"
