import type {AuthorWithKey} from '@docusaurus/plugin-content-blog';

export type HomeAuthor = AuthorWithKey & {
  name: string;
};

export type HomePost = {
  title: string;
  description: string;
  date: string;
  permalink: string;
  authorNames: string[];
};

export type HomeData = {
  authors: HomeAuthor[];
  posts: HomePost[];
};
