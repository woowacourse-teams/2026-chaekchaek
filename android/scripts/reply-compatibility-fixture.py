#!/usr/bin/env python3
"""#563 모바일 UI 검증용 합성 API. python3 scripts/reply-compatibility-fixture.py

답글 deleted 생략, 답글이 남은 삭제 감상 유지, 서버 목록과 개수 집계를 모사한다.
운영 데이터나 자격 증명을 사용하지 않는다. 전용 포트 19063에서만 실행한다.
"""
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import RLock
from urllib.parse import urlparse, parse_qs

LOCK = RLock()
STATE = {}
AUTHOR = dict(displayName='fixture reader', anonymous=True, mine=False, actorType='GUEST')
BOOK = dict(bookId=42, isbn13='9780000000042', title='Reply compatibility', authors=['fixture'],
            translators=[], publisher='fixture', category='소설', coverImageUrl='', totalPages=200)


def reset():
    STATE.update(replies=[dict(replyId=8, content='Existing reply', author=AUTHOR, likeCount=0),
                          dict(replyId=9, content='Second page reply', author=AUTHOR, likeCount=0)],
                 next_id=10, requests=[], review_deleted=False)


def review(retained=False):
    deleted = retained or STATE['review_deleted']
    replies = [dict(replyId=98, content='Retained reply', author=AUTHOR, likeCount=0)] if retained else STATE['replies']
    return dict(reviewId=99 if retained else 7,
                content='삭제된 감상입니다.' if deleted else 'Compatibility review',
                quote=None, chapter=None, createdAt='2026-10-21T00:00:00Z',
                author=AUTHOR if retained else dict(AUTHOR, mine=True), replyCount=len(replies),
                likeCount=0, recentReplies=replies[:1], deleted=deleted)


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass

    def do_GET(self):
        self.handle_request()

    def do_POST(self):
        self.handle_request()

    def do_PATCH(self):
        self.handle_request()

    def do_DELETE(self):
        self.handle_request()

    def handle_request(self):
        with LOCK:
            path = urlparse(self.path).path
            method = self.command
            body = json.loads(self.rfile.read(int(self.headers.get('Content-Length', 0))) or '{}')
            if path == '/__reply-compat/reset' and method == 'POST':
                reset()
                return self.send_json({'fixture': 'reply-compat'})
            if path == '/__reply-compat/state':
                return self.send_json(STATE)
            if path == '/__reply-compat':
                return self.send_json({'fixture': 'reply-compat'})
            STATE['requests'].append({'method': method, 'path': self.path, 'body': body})
            if path == '/api/v1/auth/guest-token':
                return self.send_json(dict(guestToken='synthetic-fixture-guest', nickname='fixture reader', expiresAt='2099-01-01T00:00:00Z'))
            if path == '/api/v1/home/popular-books':
                return self.send_json({'books': [dict(BOOK, reviewCount=1, replyCount=len(STATE['replies']))]})
            if path == '/api/v1/home/latest-reviews':
                return self.send_json({'reviews': []})
            if path == '/api/v1/books/by-isbn/9780000000042':
                return self.send_json(BOOK)
            if path == '/api/v1/books/42/reviews':
                items = [review(True)]
                if not STATE['review_deleted'] or STATE['replies']:
                    items.append(review())
                return self.send_json(dict(totalCount=len(items), items=items))
            if path == '/api/v1/reviews/7' and method == 'DELETE':
                STATE['review_deleted'] = True
                return self.send_json(None, 204)
            if path == '/api/v1/reviews/7/replies':
                if method == 'POST':
                    reply = dict(replyId=STATE['next_id'], content=body['content'],
                                 author=dict(AUTHOR, mine=True), likeCount=0)
                    STATE['next_id'] += 1
                    STATE['replies'].insert(0, reply)
                    return self.send_json(reply, 201)
                page = int(parse_qs(urlparse(self.path).query).get('page', ['1'])[0])
                items = STATE['replies'][(page - 1):page]
                return self.send_json(dict(totalCount=len(STATE['replies']), items=items,
                                           nextPage=page + 1 if page < len(STATE['replies']) else None))
            if path.startswith('/api/v1/replies/'):
                reply_id = int(path.rsplit('/', 1)[1])
                reply = next((r for r in STATE['replies'] if r['replyId'] == reply_id), None)
                if reply is None:
                    return self.send_json({'status': 404}, 404)
                if method == 'PATCH':
                    reply['content'] = body['content']
                    return self.send_json(reply)
                if method == 'DELETE':
                    STATE['replies'].remove(reply)
                    return self.send_json(None, 204)
            return self.send_json({'status': 404, 'path': path}, 404)

    def send_json(self, payload, status=200):
        data = b'' if payload is None else json.dumps(payload).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)


if __name__ == '__main__':
    reset()
    print('reply-compat fixture: http://127.0.0.1:19063', flush=True)
    ThreadingHTTPServer(('0.0.0.0', 19063), Handler).serve_forever()
