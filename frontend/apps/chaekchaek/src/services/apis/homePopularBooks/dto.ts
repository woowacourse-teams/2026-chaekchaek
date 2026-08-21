import type { ResponseDto } from '@/services/apis/api.types';

export interface GetHomePopularBooksRequestDto {}

export type GetHomePopularBooksResponseDto = ResponseDto<{
  books: {
    replyCount: number;
    reviewCount: number;
    coverImageUrl: string;
    isbn13: string;
    title: string;
    authors: string[];
    bookId: number;
  }[];
}>;
