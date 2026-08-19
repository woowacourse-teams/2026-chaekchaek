import type { ResponseDto } from '@/services/apis/api.types';

export interface GetBooksRequestDto {
  query: { query: string; page: number };
  undefined;
}

export type GetBooksResponseDto = ResponseDto<
  200,
  {
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
      commentCount: number;
      authors: string[];
      bookId: number;
    }[];
  }
>;
