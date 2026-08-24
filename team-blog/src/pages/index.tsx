import type {ReactNode} from 'react';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import {usePluginData} from '@docusaurus/useGlobalData';
import AuthorSocials from '@theme/Blog/Components/Author/Socials';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';
import type {HomeAuthor, HomeData, HomePost} from '../types/home';

const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
});

function AuthorCard({author}: {author: HomeAuthor}): ReactNode {
  const profile = (
    <div className="card__body">
      <div className="avatar">
        {author.imageURL && (
          <img
            className="avatar__photo"
            src={author.imageURL}
            alt={`${author.name} 프로필 사진`}
          />
        )}
        <div className="avatar__intro">
          <Heading as="h3" className="avatar__name">
            {author.url ? <Link to={author.url}>{author.name}</Link> : author.name}
          </Heading>
          {author.title && <small className="avatar__subtitle">{author.title}</small>}
          {author.socials && <AuthorSocials author={author} />}
        </div>
      </div>
      {author.description && <p>{author.description}</p>}
    </div>
  );

  return (
    <article className="col col--4 margin-bottom--lg">
      <div className="card">{profile}</div>
    </article>
  );
}

function PostCard({post}: {post: HomePost}): ReactNode {
  return (
    <article className="col col--4 margin-bottom--lg">
      <div className="card">
        <div className="card__body">
          <Heading as="h3">
            <Link to={post.permalink}>{post.title}</Link>
          </Heading>
          <small>
            <time dateTime={post.date}>{dateFormatter.format(new Date(post.date))}</time>
            {post.authorNames.length > 0 && ` · ${post.authorNames.join(', ')}`}
          </small>
          <p>{post.description}</p>
        </div>
        <div className="card__footer">
          <Link to={post.permalink}>자세히 보기</Link>
        </div>
      </div>
    </article>
  );
}

export default function Home(): ReactNode {
  const {siteConfig} = useDocusaurusContext();
  const {authors, posts} = usePluginData('team-blog-home', undefined, {
    failfast: true,
  }) as HomeData;

  return (
    <Layout>
      <header className="hero hero--primary">
        <div className="container">
          <Heading as="h1" className="hero__title">
            {siteConfig.title}
          </Heading>
          <p className="hero__subtitle">{siteConfig.tagline}</p>
          <div>
            <Link className="button button--secondary button--lg" to="/blog">
              글 모아보기
            </Link>
          </div>
        </div>
      </header>
      <main className="container margin-vert--xl">
        {authors.length > 0 && (
          <section className="margin-bottom--xl">
            <div className="row margin-bottom--lg">
              <div className="col">
                <Heading as="h2">팀원</Heading>
                <p>책췍의 기록을 함께 만드는 사람들입니다.</p>
              </div>
              <div className="col col--auto">
                <Link to="/blog/authors">팀원 전체 보기</Link>
              </div>
            </div>
            <div className="row">{authors.map((author) => <AuthorCard author={author} key={author.key} />)}</div>
          </section>
        )}

        {posts.length > 0 && (
          <section className="margin-bottom--xl">
            <div className="row margin-bottom--lg">
              <div className="col">
                <Heading as="h2">최근 글</Heading>
                <p>가장 최근에 공개된 글을 최대 3개까지 보여 줍니다.</p>
              </div>
              <div className="col col--auto">
                <Link to="/blog">글 모아보기</Link>
              </div>
            </div>
            <div className="row">{posts.map((post) => <PostCard post={post} key={post.permalink} />)}</div>
          </section>
        )}
      </main>
    </Layout>
  );
}
