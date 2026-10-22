#!/usr/bin/env python3
"""A cross-encoder behind one HTTP endpoint, for the Java side of Episode 5.

Running a cross-encoder in-process in Java means exporting the model to ONNX and carrying a
tokenizer with it, which is an episode of its own. This is the smaller answer: sentence-transformers
loads the model once, and ``CrossEncoderReranker`` posts a question and its passages to it. The
scores are then the same numbers the prototype gets, from the same library and the same weights,
which is what makes the Java-against-prototype parity check mean anything.

    pip install sentence-transformers
    tools/rerank-service/serve.py                       # 127.0.0.1:8099, MiniLM
    MODEL=... PORT=... tools/rerank-service/serve.py

    GET  /health   -> {"model": ..., "max_length": 512}
    POST /rerank   {"query": str, "passages": [str]} -> {"model": ..., "scores": [float]}

Scores are the model's own scale: higher is more relevant, and they are not comparable with BM25,
a cosine or another reranker's.
"""

import json
import os
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

MODEL = os.environ.get("MODEL", "cross-encoder/ms-marco-MiniLM-L-6-v2")
PORT = int(os.environ.get("PORT", "8099"))
HOST = os.environ.get("HOST", "127.0.0.1")
MAX_LENGTH = int(os.environ.get("MAX_LENGTH", "512"))
BATCH = int(os.environ.get("BATCH", "32"))

try:
    from sentence_transformers import CrossEncoder
except ImportError:
    sys.exit("sentence-transformers is not installed: pip install sentence-transformers")

print(f"loading {MODEL} (max_length={MAX_LENGTH})", file=sys.stderr)
model = CrossEncoder(MODEL, max_length=MAX_LENGTH)
cache: dict = {}  # (question, passage) -> score, as the prototype's reranker caches too


def scores(query: str, passages: list) -> list:
    todo = [p for p in passages if (query, p) not in cache]
    if todo:
        out = model.predict([(query, p) for p in todo], batch_size=BATCH, show_progress_bar=False)
        for p, s in zip(todo, out):
            cache[(query, p)] = float(s)
    return [cache[(query, p)] for p in passages]


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def _send(self, code: int, payload: dict):
        body = json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path.rstrip("/") == "/health":
            self._send(200, {"model": MODEL, "max_length": MAX_LENGTH})
        else:
            self._send(404, {"error": "try /health or POST /rerank"})

    def do_POST(self):
        if self.path.rstrip("/") != "/rerank":
            self._send(404, {"error": "try POST /rerank"})
            return
        try:
            body = json.loads(self.rfile.read(int(self.headers.get("Content-Length", "0"))) or b"{}")
            query, passages = body["query"], body["passages"]
            if not isinstance(query, str) or not isinstance(passages, list):
                raise ValueError("query must be a string and passages a list of strings")
        except (ValueError, KeyError, TypeError) as e:
            self._send(400, {"error": f"{type(e).__name__}: {e}"})
            return
        try:
            self._send(200, {"model": MODEL, "scores": scores(query, passages)})
        except Exception as e:  # a model failure is the caller's problem to see, not a silent zero
            self._send(500, {"error": f"{type(e).__name__}: {e}"})

    def log_message(self, fmt, *args):  # one line per request is noise at 53 questions a run
        pass


if __name__ == "__main__":
    print(f"reranking on http://{HOST}:{PORT} with {MODEL}", file=sys.stderr)
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
