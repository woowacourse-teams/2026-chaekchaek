export type HomeAuthor = {
  key: string;
  name: string;
  title?: string;
  url?: string;
  imageUrl?: string;
  description?: string;
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
