#!/usr/bin/env python3
"""Fetch a handful of genuinely public newsroom pages, politely, for face validity.

LIMITS, stated here because they are part of the result:
  * This is a MECHANISM check only. It asks whether the extract-then-query
    pipeline behaves the same way on real prose. It does NOT and must NOT be
    used to assert which real company supplies which. Any such inference from a
    handful of press releases would be commercially sensitive and unsound.
  * A handful of pages, one request at a time, with a delay between requests,
    a descriptive User-Agent, and no crawling: only the exact URLs given.
  * Downloads land in their own directory and are treated as untrusted data.
    Scripts live elsewhere and receive paths as arguments.

Usage:
  python3 realdata_fetch.py --urls realdata_urls.txt --dest realdata_downloads
"""

import argparse
import html
import os
import re
import time
import urllib.request
import urllib.error

UA = ("rags-spring-ai-graph-spike/0.1 (research spike; one-off, rate-limited; "
      "contact: me@briancorbin.co.uk)")


def strip_html(raw):
    s = re.sub(r"(?is)<(script|style|noscript|svg)[^>]*>.*?</\1>", " ", raw)
    s = re.sub(r"(?is)<br\s*/?>|</p>|</div>|</li>|</h[1-6]>", "\n", s)
    s = re.sub(r"(?s)<[^>]+>", " ", s)
    s = html.unescape(s)
    s = re.sub(r"[ \t\xa0]+", " ", s)
    s = re.sub(r"\n\s*\n\s*\n+", "\n\n", s)
    return s.strip()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--urls", required=True)
    ap.add_argument("--dest", required=True)
    ap.add_argument("--delay", type=float, default=6.0)
    a = ap.parse_args()

    os.makedirs(a.dest, exist_ok=True)
    urls = [l.strip() for l in open(a.urls)
            if l.strip() and not l.strip().startswith("#")]
    print("%d urls, %.1fs delay, dest=%s" % (len(urls), a.delay, a.dest))

    log = []
    for i, u in enumerate(urls, 1):
        name = "doc%02d" % i
        try:
            req = urllib.request.Request(u, headers={
                "User-Agent": UA, "Accept": "text/html,application/xhtml+xml"})
            with urllib.request.urlopen(req, timeout=45) as r:
                raw = r.read().decode("utf-8", "replace")
                code = r.status
            with open(os.path.join(a.dest, name + ".html"), "w") as f:
                f.write(raw)
            txt = strip_html(raw)
            with open(os.path.join(a.dest, name + ".txt"), "w") as f:
                f.write(txt)
            print("  [%d] %s -> %s  http=%s  %d chars text"
                  % (i, u[:70], name, code, len(txt)))
            log.append({"url": u, "file": name, "status": code, "chars": len(txt)})
        except Exception as e:
            print("  [%d] %s -> FAILED: %s" % (i, u[:70], e))
            log.append({"url": u, "file": None, "status": "error", "error": str(e)})
        if i < len(urls):
            time.sleep(a.delay)

    import json
    with open(os.path.join(a.dest, "fetch_log.json"), "w") as f:
        json.dump(log, f, indent=1)
    ok = sum(1 for x in log if x.get("file"))
    print("fetched %d/%d" % (ok, len(urls)))


if __name__ == "__main__":
    main()
