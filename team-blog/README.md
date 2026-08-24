# 책책 기술 블로그

Docusaurus 기반의 책책 팀 기술 블로그입니다. 소스는 모노레포의 `blog` 브랜치에서 관리하며, GitHub Actions가 GitHub Pages로 배포합니다.

## 시작하기

```bash
cd team-blog
npm ci
npm run start
```

브라우저에서 로컬 개발 서버 주소를 열면 변경 사항이 즉시 반영됩니다.

## 글 작성

1. `blog`에서 `blog-post/<글-주제>` 브랜치를 만듭니다.
2. `blog/YYYY-MM-DD-글-제목.md` 파일을 추가합니다.
3. `blog/authors.yml`과 `blog/tags.yml`의 식별자를 front matter에서 사용합니다.
4. 아래 검증을 통과시킨 뒤 `blog`을 대상으로 PR을 만듭니다.

```bash
npm run build
```

## 이미지

글 전용 이미지는 `static/img/posts/<글-주제>/`에 둡니다. 본문에서는 `/img/posts/<글-주제>/이미지명.png`처럼 참조합니다.

## 배포

`blog`으로 병합되면 `.github/workflows/blog-deploy.yml`이 사이트를 빌드해 GitHub Pages에 배포합니다. 배포 결과물용 `gh-pages` 브랜치는 사용하지 않습니다.
