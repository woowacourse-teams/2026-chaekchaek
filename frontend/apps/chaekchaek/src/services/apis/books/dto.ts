import type { ResponseDto } from '@/services/apis/api.types';

export interface GetBooksRequestDto {
  query: { query: string; page: number };
}

export type GetBooksResponseDto = ResponseDto<{
  nextPage: number;
  totalCount: number;
  items: {
    translators: string[];
    coverImageUrl: string;
    isbn13: string;
    publisher: string;
    publishedDate: string;
    category: string;
    title: string;
    reviewCount: number | null;
    replyCount: number | null;
    authors: string[];
    bookId: number;
  }[];
}>;
