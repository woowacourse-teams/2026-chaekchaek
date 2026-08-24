import type {AllContent, LoadContext, Plugin} from '@docusaurus/types';
import type {
  AuthorsMap,
  BlogContent,
} from '@docusaurus/plugin-content-blog';
import type {HomeAuthor, HomeData, HomePost} from '../types/home';

const pluginName = 'team-blog-home';

function toHomeAuthors(authorsMap: AuthorsMap | undefined): HomeAuthor[] {
  return Object.values(authorsMap ?? {}).map((author) => ({
    ...author,
    name: author.name ?? author.key,
  }));
}

function toRecentPosts(blogPosts: BlogContent['blogPosts']): HomePost[] {
  return blogPosts
    .filter((post) => !post.metadata.unlisted)
    .sort((first, second) => second.metadata.date.getTime() - first.metadata.date.getTime())
    .slice(0, 3)
    .map((post) => ({
      title: post.metadata.title,
      description: post.metadata.description,
      date: post.metadata.date.toISOString(),
      permalink: post.metadata.permalink,
      authorNames: post.metadata.authors.map(
        (author) => author.name ?? '책췍 팀원',
      ),
    }));
}

function getBlogContent(allContent: AllContent): BlogContent {
  const blogContent = allContent['docusaurus-plugin-content-blog']?.default;

  if (!blogContent) {
    throw new Error('팀 블로그 홈 데이터를 찾을 수 없습니다.');
  }

  return blogContent as BlogContent;
}

export default function teamBlogHomePlugin(_context: LoadContext): Plugin {
  return {
    name: pluginName,
    allContentLoaded({allContent, actions}) {
      const blogContent = getBlogContent(allContent);
      const homeData: HomeData = {
        authors: toHomeAuthors(blogContent.authorsMap),
        posts: toRecentPosts(blogContent.blogPosts),
      };

      actions.setGlobalData(homeData);
    },
  };
}
