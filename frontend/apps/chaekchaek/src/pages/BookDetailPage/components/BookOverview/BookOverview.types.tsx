export type Props = {
  category: string | undefined;
  publishedDate: string | undefined;
  title: string | undefined;
  authors: readonly unknown[] | undefined;
  publisher: string | undefined;
  description: string | undefined;
  averageRating: number | undefined;
  reviewCount: number | undefined;
  replyCount: number | undefined;
  coverImageUrl: string | undefined;
};
