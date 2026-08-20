export interface GetMembersMeRatingsComparisonParams {
  isbn13: string;
  criterion: number;
}

export type GetMembersMeRatingsComparison = (
  params: GetMembersMeRatingsComparisonParams,
) => Promise<{
  current: {
    myRating: number;
    coverImageUrl: string;
    isbn13: string;
    title: string;
    authors: string[];
    bookId: number;
  };
  lower: {
    myRating: number;
    coverImageUrl: string;
    isbn13: string;
    title: string;
    authors: string[];
    bookId: number;
  };
  higher: {
    myRating: number;
    coverImageUrl: string;
    isbn13: string;
    title: string;
    authors: string[];
    bookId: number;
  };
}>;
