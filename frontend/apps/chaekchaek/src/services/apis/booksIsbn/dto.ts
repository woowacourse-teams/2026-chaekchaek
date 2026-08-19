import type { ResponseDto } from '@/services/apis/api.types';

export interface GetBooksIsbnRequestDto {
  pathParams: [{ name: 'bookId'; value: number }];
}

export type GetBooksIsbnResponseDto = ResponseDto<{
  myRecord: {
    myRating: number;
    currentPage: number;
    status: string;
  };
  coverImageUrl: string;
  ratingCount: number;
  title: string;
  commentCount: number;
  bookId: number;
  translators: undefined[];
  averageRating: number;
  isbn13: string;
  totalPages: number;
  publisher: string;
  publishedDate: string;
  category: string;
  authors: undefined[];
}>;
