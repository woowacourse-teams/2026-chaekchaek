#!/usr/bin/env python3
"""최신 책장 계약의 모바일 검증용 합성 서버. 운영 계정과 데이터는 사용하지 않는다."""
import argparse
import json
import re
import struct
import zlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import RLock
from urllib.parse import urlparse, parse_qs

LOCK = RLock()
STATE = {}
BOOKS = {i: dict(bookId=i, isbn13=f'97800000000{i:02}', title=f'검증 도서 {i}', authors=['검증 저자'],
                coverImageUrl='', spineImageUrl=None, totalPages=200, status='READING', currentPage=20,
                readingUpdatedAt='2026-10-23T00:00:00Z', publisher='검증 출판', category='소설') for i in range(1, 37)}


def cover_png(variant=0, width=48):
    """표지색 추출을 확인하는 합성 표지. 첨부 원본 이미지와는 별개다."""
    stops = [(0.08, (37, 60, 94)), (0.38, (151, 127, 118)), (0.90, (214, 167, 131))]
    if variant == 1:
        stops = [(0.08, (24, 85, 52)), (0.38, (99, 164, 111)), (0.90, (224, 230, 173))]
    elif variant == 2:
        stops = [(0.08, (150, 30, 45)), (0.38, (230, 120, 95)), (0.90, (252, 220, 190))]
    rows = []
    for y in range(96):
        position = y / 95
        lower = max((stop for stop in stops if stop[0] <= position), default=stops[0])
        upper = min((stop for stop in stops if stop[0] >= position), default=stops[-1])
        amount = 0 if lower[0] == upper[0] else (position - lower[0]) / (upper[0] - lower[0])
        rgb = bytes(round(a + (b - a) * amount) for a, b in zip(lower[1], upper[1]))
        rows.append(b'\x00' + rgb * width)
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, 96, 8, 2, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(b''.join(rows))) + chunk(b'IEND', b''))


def preparing_spine_png():
    """HTTP 200으로 내려오는 YES24 이미지 준비중 책등을 재현하는 합성 이미지."""
    width, height = 420, 600
    rows = []
    for y in range(height):
        row = bytearray()
        for x in range(width):
            rgb = (208, 208, 208) if 140 <= x <= 280 and 240 <= y <= 360 else (255, 255, 255)
            row.extend(rgb)
        rows.append(b'\x00' + bytes(row))
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(b''.join(rows))) + chunk(b'IEND', b''))


COVERS = {f'/assets/cover-{i}.png': cover_png(i) for i in range(3)}
COVERS['/assets/original-spine.png'] = cover_png(2, width=12)
COVERS['/assets/preparing-spine.png'] = preparing_spine_png()


def reset():
    STATE.clear()
    STATE.update(shelves=[dict(libraryId=1, name='검증의 책장', isDefault=True, books=[1, 2]),
                          dict(libraryId=2, name='다시 읽기', isDefault=False, books=[3]),
                          dict(libraryId=3, name='이전 35권', isDefault=False, books=list(range(1, 36)))],
                 next_id=4, requests=[], fail_next=False)


def summary(shelf):
    return dict(libraryId=shelf['libraryId'], name=shelf['name'], isDefault=shelf['isDefault'],
                totalCount=len(shelf['books']), items=[{k: BOOKS[i][k] for k in ('bookId', 'isbn13', 'title', 'coverImageUrl', 'authors', 'spineImageUrl')} for i in shelf['books']])


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass

    def do_GET(self): self.handle_request()
    def do_POST(self): self.handle_request()
    def do_PATCH(self): self.handle_request()
    def do_DELETE(self): self.handle_request()

    def handle_request(self):
        with LOCK:
            path = urlparse(self.path).path
            query = parse_qs(urlparse(self.path).query)
            method = self.command
            if path in COVERS:
                data = COVERS[path]
                self.send_response(200)
                self.send_header('Content-Type', 'image/png')
                self.send_header('Content-Length', str(len(data)))
                self.end_headers()
                self.wfile.write(data)
                return
            for book in BOOKS.values():
                origin = f'http://{self.headers.get("Host")}'
                book['coverImageUrl'] = f'{origin}/assets/cover-{(book["bookId"] - 1) % 3}.png'
                book['spineImageUrl'] = (f'{origin}/assets/preparing-spine.png' if book['bookId'] == 1 else
                                         f'{origin}/assets/original-spine.png' if book['bookId'] % 3 == 0 else None)
            body = json.loads(self.rfile.read(int(self.headers.get('Content-Length', 0))) or '{}')
            if path == '/__bookshelf/reset':
                reset()
                return self.send_json({'fixture': 'bookshelf-contract'})
            if path == '/__bookshelf/state': return self.send_json(STATE)
            if path == '/__bookshelf/fail-next':
                STATE['fail_next'] = True
                return self.send_json({})
            if path == '/__bookshelf': return self.send_json({'fixture': 'bookshelf-contract'})
            STATE['requests'].append(dict(method=method, path=self.path, body=body))
            if STATE['fail_next'] and method in ('POST', 'PATCH', 'DELETE') and path.startswith('/api/v1/libraries'):
                STATE['fail_next'] = False
                return self.send_json({'code': 'INTERNAL_SERVER_ERROR'}, 500)
            if path.startswith('/api/v1/auth/mobile/'):
                return self.send_json(dict(accessToken='synthetic-bookshelf-member', refreshToken='synthetic-bookshelf-refresh', tokenType='Bearer', accessTokenExpiresIn=3600, refreshTokenExpiresIn=86400))
            if path == '/api/v1/members/me':
                return self.send_json(dict(memberId=9, nickname='검증', anonymousNickname='검증 참새', displayAnonymous=False))
            if path == '/api/v1/auth/guest-token':
                return self.send_json(dict(guestToken='synthetic-bookshelf-guest', nickname='검증 참새', expiresAt='2099-01-01T00:00:00Z'))
            if path == '/api/v1/home/popular-books': return self.send_json({'books': [dict(BOOKS[36], reviewCount=0, replyCount=0)]})
            if path == '/api/v1/home/latest-reviews': return self.send_json({'reviews': []})
            if path == '/api/v1/home/reading-books': return self.send_json({'books': []})
            if path == '/api/v1/feed/reviews': return self.send_json(dict(reviews=[], totalCount=0, nextPage=None))
            if path in ('/api/v1/reviews', '/api/v1/feed'): return self.send_json(dict(items=[], totalCount=0, nextPage=None))
            if path == '/api/v1/books': return self.send_json(dict(items=[dict(BOOKS[36], translators=[], bookId=None, category=None, publishedDate=None)], totalCount=1, nextPage=None))
            if path.startswith('/api/v1/books/by-isbn/'):
                book = next((b for b in BOOKS.values() if b['isbn13'] == path.rsplit('/', 1)[1]), BOOKS[36])
                return self.send_json(dict(book, translators=[], myRecord=dict(bookId=book['bookId'], status='READING', currentPage=20, rating=None)))
            if re.fullmatch(r'/api/v1/books/\d+/reviews', path):
                book_id = int(path.split('/')[4])
                if book_id % 2 == 0: return self.send_json(dict(items=[], totalCount=0, nextPage=None))
                review = dict(reviewId=book_id, content=f'검증 도서 {book_id}의 최신 감상', quote=None, chapter=None,
                              createdAt='2026-10-23T01:00:00Z', author=dict(displayName='검증', anonymous=False, mine=True, actorType='MEMBER'), replyCount=0, likeCount=0, deleted=False)
                return self.send_json(dict(items=[review], totalCount=1, nextPage=None))
            if path == '/api/v1/library':
                if method == 'GET': return self.send_json(dict(items=[BOOKS[i] for i in STATE['shelves'][0]['books']], totalCount=len(STATE['shelves'][0]['books']), nextPage=None))
            if path == '/api/v1/libraries' and method == 'GET':
                return self.send_json(dict(memberId=9, libraries=[summary(s) for s in STATE['shelves']]))
            if path == '/api/v1/libraries' and method == 'POST':
                if any(s['name'].casefold() == body.get('name', '').casefold() for s in STATE['shelves']): return self.problem('LIBRARY_NAME_ALREADY_EXISTS')
                shelf = dict(libraryId=STATE['next_id'], name=body['name'], isDefault=False, books=[])
                STATE['next_id'] += 1
                STATE['shelves'].append(shelf)
                return self.send_json(summary(shelf), 201)
            if path == '/api/v1/libraries/order':
                ids = body['libraryIds']
                if sorted(ids) != sorted(s['libraryId'] for s in STATE['shelves']): return self.problem('ORDER_CONFLICT')
                STATE['shelves'].sort(key=lambda s: ids.index(s['libraryId']))
                return self.send_json(None, 204)
            match = re.fullmatch(r'/api/v1/libraries/(\d+)(?:/books/(order|\d+))?', path)
            if match:
                shelf = next((s for s in STATE['shelves'] if s['libraryId'] == int(match[1])), None)
                if shelf is None: return self.send_json({'code': 'LIBRARY_NOT_FOUND'}, 404)
                if match[2] == 'order':
                    if sorted(body['bookIds']) != sorted(shelf['books']): return self.problem('ORDER_CONFLICT')
                    shelf['books'] = body['bookIds']
                    return self.send_json(None, 204)
                if match[2]:
                    shelf['books'].remove(int(match[2]))
                    return self.send_json(None, 204)
                if method == 'GET': return self.send_json(dict(summary(shelf), memberId=9, items=[BOOKS[i] for i in shelf['books']]))
                if method == 'PATCH':
                    if any(s['libraryId'] != shelf['libraryId'] and s['name'].casefold() == body['name'].casefold() for s in STATE['shelves']): return self.problem('LIBRARY_NAME_ALREADY_EXISTS')
                    shelf['name'] = body['name']
                    return self.send_json(None, 204)
                if method == 'DELETE':
                    if shelf['isDefault']: return self.problem('DEFAULT_LIBRARY_CANNOT_DELETE')
                    STATE['shelves'].remove(shelf)
                    return self.send_json(None, 204)
                if method == 'POST':
                    if body.get('status') not in ('WANT_TO_READ', 'READING', 'FINISHED'):
                        return self.send_json({'code': 'INVALID_REQUEST'}, 400)
                    book_id = next(i for i, b in BOOKS.items() if b['isbn13'] == body['isbn13'])
                    if book_id in shelf['books']: return self.problem('LIBRARY_ITEM_ALREADY_EXISTS')
                    if len(shelf['books']) >= 30: return self.problem('LIBRARY_ITEM_LIMIT_EXCEEDED')
                    shelf['books'].append(book_id)
                    return self.send_json(dict(bookId=book_id, status='READING', currentPage=20, rating=4.5), 201)
            return self.send_json({'code': 'NOT_FOUND', 'path': path}, 404)

    def problem(self, code): return self.send_json({'code': code, 'status': 409}, 409)

    def send_json(self, payload, status=200):
        data = b'' if payload is None else json.dumps(payload, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=19068)
    args = parser.parse_args()
    reset()
    print(f'bookshelf-contract fixture port: {args.port}', flush=True)
    ThreadingHTTPServer(('127.0.0.1', args.port), Handler).serve_forever()
