#!/usr/bin/env python3
"""#607 전용 공개 조회 응답. 운영 데이터와 쓰기 API를 사용하지 않는다."""
import argparse
import json
from collections import Counter
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit, parse_qs

COUNTS = Counter()
def author(name="독자A", member=42, anonymous=False, actor="MEMBER", available=True):
    return dict(memberId=member, displayName=name, profileImageUrl=None, anonymous=anonymous,
                mine=False, actorType=actor, profileStatus="AVAILABLE" if available else "UNAVAILABLE")
AUTHORS = [author(), author("빈서재독자", 43), author("오류서재독자", 44),
           author("익명독자", None, True, available=False),
           author("비회원독자", None, actor="GUEST", available=False),
           author("탈퇴한 사용자", None, available=False)]
def book(i):
    return dict(bookId=i, isbn13=f"978000000{i:04d}", title=f"서재 도서 {i}",
                coverImageUrl="", spineImageUrl=None, authors=["검증저자"], totalPages=300,
                status="READING" if i % 2 else "FINISHED", currentPage=i,
                readingUpdatedAt="2026-10-23T00:00:00Z")
def review(i, profile=None):
    return dict(reviewId=i, content="공개 서재를 확인하는 감상", quote=None, chapter=None, currentPage=None,
                createdAt=f"2026-10-23T00:00:{i:02d}Z", author=profile or author(), replyCount=1,
                likeCount=0, likedByMe=False, isSpoiler=False, deleted=False,
                recentReplies=[dict(replyId=100+i, content="답글 작성자도 서재로 이동", author=author("답글독자", 42),
                                    likeCount=0, likedByMe=False, deleted=False)],
                bookId=1, isbn13="9780000000001", bookTitle="서재 도서 1", bookCoverImageUrl="")
def response(path, query):
    if path == "/__607/reset": COUNTS.clear(); return 200, {"marker":"607"}
    if path == "/__607": return 200, {"marker":"607", "counts":dict(COUNTS)}
    if path == "/api/v1/home/popular-books":
        return 200, {"books":[dict(bookId=1,isbn13="9780000000001",title="서재 도서 1",coverImageUrl="",reviewCount=6,replyCount=1)]}
    if path == "/api/v1/home/latest-reviews": return 200, {"reviews":[review(i+1,a) for i,a in enumerate(AUTHORS)]}
    if path == "/api/v1/feed/reviews": return 200, {"reviews":[review(i+1,a) for i,a in enumerate(AUTHORS)],"totalCount":6,"nextPage":None}
    if path == "/api/v1/libraries":
        member = int(query.get("memberId", [42])[0])
        if member == 44 and COUNTS[path+"?memberId=44"] == 1: return 503, {"code":"UNAVAILABLE"}
        if member == 43: return 200, {"memberId":43,"libraries":[]}
        return 200, {"memberId":member,"libraries":[dict(libraryId=9,name="기본책장",isDefault=True,totalCount=35,items=[book(i) for i in range(1,36)]),dict(libraryId=10,name="빈책장",isDefault=False,totalCount=0,items=[])]}
    if path in ("/api/v1/libraries/9", "/api/v1/libraries/10"):
        empty = path.endswith("/10")
        return 200, dict(libraryId=10 if empty else 9, memberId=42,name="빈책장" if empty else "기본책장",isDefault=not empty,totalCount=0 if empty else 35,items=[] if empty else [book(i) for i in range(1,36)])
    if "/replies" in path: return 200, {"items":[review(1)["recentReplies"][0]],"totalCount":1,"nextPage":None}
    if path.endswith("/reviews"): return 200, {"items":[review(1)],"totalCount":1,"nextPage":None}
    if path.startswith("/api/v1/books/by-isbn/"):
        isbn = path.rsplit("/",1)[-1]; i=int(isbn[-4:])
        return 200, dict(book(i),isbn13=isbn,translators=[],publisher="검증출판사",category="소설",publishedDate=None,myRecord=None,description="공개 서재 도서 상세")
    return 200, {"items":[],"totalCount":0,"nextPage":None}
class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        url=urlsplit(self.path); query=parse_qs(url.query)
        key=url.path+("?memberId="+query["memberId"][0] if url.path=="/api/v1/libraries" else "")
        COUNTS[key]+=1
        status, body=response(url.path,query)
        data=json.dumps(body,ensure_ascii=False).encode()
        self.send_response(status); self.send_header("Content-Type","application/json"); self.send_header("Content-Length",str(len(data)));self.send_header("Connection","close");self.end_headers();self.wfile.write(data)
    def log_message(self,*args): pass
if __name__=="__main__":
    parser=argparse.ArgumentParser();parser.add_argument("--port",type=int,default=19087);args=parser.parse_args()
    ThreadingHTTPServer(("127.0.0.1",args.port),Handler).serve_forever()
