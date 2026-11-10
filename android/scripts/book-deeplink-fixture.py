#!/usr/bin/env python3
"""#692 양 플랫폼 딥링크 검증용 읽기 전용 합성 API. 운영 API와 인증을 사용하지 않는다."""
import argparse
import json
from collections import Counter
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Lock
from urllib.parse import urlsplit

COUNTS = Counter()
LOCK = Lock()
MODE = "normal"
VALID = "9788937460449"
MISSING = "9791172376215"
RETRY = "9788936433598"


def response(path):
    global MODE
    if path == "/__692/reset":
        COUNTS.clear()
        MODE = "normal"
        return 200, {"fixture": "692", "mode": MODE}
    if path == "/__692/mode/missing":
        MODE = "missing"
        return 200, {"fixture": "692", "mode": MODE}
    if path == "/__692/mode/retry":
        MODE = "retry"
        return 200, {"fixture": "692", "mode": MODE}
    if path == "/__692/mode/normal":
        MODE = "normal"
        return 200, {"fixture": "692", "mode": MODE}
    if path == "/__692":
        return 200, {"fixture": "692", "counts": dict(COUNTS), "mode": MODE}
    COUNTS[path] += 1
    if path.startswith("/api/v1/books/by-isbn/"):
        isbn = path.rsplit("/", 1)[-1]
        if MODE == "missing" or isbn == MISSING:
            return 404, {"code": "BOOK_NOT_FOUND"}
        if (MODE == "retry" and COUNTS[path] == 1) or (isbn == RETRY and COUNTS[path] == 1):
            return 503, {"code": "UNAVAILABLE"}
        if isbn not in (VALID, RETRY):
            return 400, {"code": "INVALID_ISBN"}
        return 200, dict(bookId=692, isbn13=isbn, title="딥링크 검증 도서", authors=["검증 저자"],
                         translators=[], publisher="검증 출판사", category="소설", totalPages=200,
                         publishedDate="2026-11-09", coverImageUrl="", myRecord=None)
    if path == "/api/v1/home/popular-books":
        return 200, {"books": []}
    if path == "/api/v1/home/latest-reviews":
        return 200, {"reviews": []}
    if path == "/api/v1/feed/reviews":
        return 200, {"reviews": [], "totalCount": 0, "nextPage": None}
    if path == "/api/v1/pmf-survey":
        return 200, {"eligible": False, "responded": False, "questions": []}
    if path.endswith("/reviews"):
        return 200, {"items": [], "totalCount": 0, "nextPage": None}
    return 200, {"items": [], "totalCount": 0, "nextPage": None}


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        with LOCK:
            status, body = response(urlsplit(self.path).path)
        data = json.dumps(body, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Connection", "close")
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, *_):
        pass


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=19092)
    ThreadingHTTPServer(("127.0.0.1", parser.parse_args().port), Handler).serve_forever()
