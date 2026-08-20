import type { ResponseDto } from '@/services/apis/api.types';

export interface GetMembersMeRatingsComparisonRequestDto {
  query: { isbn13: string; criterion: number };
}

export type GetMembersMeRatingsComparisonResponseDto = ResponseDto<{
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
