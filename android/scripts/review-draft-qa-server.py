"""감상 저장 실패와 재시도 검증용 로컬 API.

python3 android/scripts/review-draft-qa-server.py
Xcode build-for-testing에 CHAEKCHAEK_API_ENVIRONMENT=local을 빌드 설정으로 전달하고
LoginRequiredSheetUITests/testReviewDraftSurvivesFailureAndClosesAfterRetry를 실행한다.
처음 두 감상 저장은 500, 이후 저장은 201로 응답한다. 운영 API나 사용자 자격 증명은 쓰지 않는다.
"""

import json
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


class ReviewDraftFixture(BaseHTTPRequestHandler):
    submissions = []
    lock = threading.Lock()
    review = {
        "reviewId": 7,
        "content": "draft must survive",
        "quote": "saved quote",
        "chapter": None,
        "createdAt": "2026-10-16T00:00:00Z",
        "author": {"displayName": "검증 게스트", "anonymous": False, "mine": True, "actorType": "GUEST"},
        "replyCount": 0,
        "likeCount": 0,
        "likedByMe": False,
        "isSpoiler": True,
        "recentReplies": [],
        "deleted": False,
    }

    def log_message(self, *_):
        pass

    def respond(self, value, status=200):
        data = json.dumps(value, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        path = self.path.split("?")[0]
        if path == "/__review-draft-qa":
            return self.respond({"fixture": "review-draft"})
        if path == "/__review-draft-qa/state":
            with self.lock:
                return self.respond({"requests": list(self.submissions)})
        if path == "/api/v1/home/popular-books":
            return self.respond({"books": [{
                "bookId": 42, "isbn13": "9780000000042", "title": "감상 보존 검증",
                "coverImageUrl": "", "reviewCount": 0, "replyCount": 0,
            }]})
        if path == "/api/v1/home/latest-reviews":
            return self.respond({"reviews": []})
        if path == "/api/v1/books/42/reviews":
            with self.lock:
                saved = len(self.submissions) > 2
            return self.respond({"totalCount": int(saved), "nextPage": None, "items": [self.review] if saved else []})
        if path == "/api/v1/books/by-isbn/9780000000042":
            return self.respond({
                "bookId": 42, "isbn13": "9780000000042", "title": "감상 보존 검증",
                "authors": [], "translators": [], "publisher": "테스트", "category": "소설",
                "coverImageUrl": "", "totalPages": 224, "myRecord": None,
            })
        self.respond({}, 404)

    def do_POST(self):
        path = self.path.split("?")[0]
        body = json.loads(self.rfile.read(int(self.headers.get("Content-Length", "0"))) or b"{}")
        if path == "/__review-draft-qa/reset":
            with self.lock:
                self.submissions.clear()
            return self.respond({})
        if path == "/api/v1/auth/guest-token":
            return self.respond({
                "guestToken": "qa-fixture-only", "nickname": "검증 게스트", "expiresAt": "2099-01-01T00:00:00Z",
            })
        if path in ("/api/v1/books/by-isbn/9780000000042/reviews", "/api/v1/books/42/reviews"):
            with self.lock:
                self.submissions.append(body)
                attempt = len(self.submissions)
            time.sleep(1)
            if attempt <= 2:
                return self.respond({"message": "fixture failure"}, 500)
            response = {"bookId": 42, "review": self.review} if "/by-isbn/" in path else self.review
            return self.respond(response, 201)
        self.respond({}, 404)


if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", 9090), ReviewDraftFixture).serve_forever()
