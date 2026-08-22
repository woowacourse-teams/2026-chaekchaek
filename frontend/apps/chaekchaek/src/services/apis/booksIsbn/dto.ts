import type { ResponseDto } from '@/services/apis/api.types';

export interface GetBooksIsbnRequestDto {
  pathParams: [{ name: 'isbn'; value: string }];
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
  bookId: number;
  translators: undefined[];
  averageRating: number;
  isbn13: string;
  totalPages: number;
  publisher: string;
  publishedDate: string;
  category: string;
  authors: undefined[];
  description: string;
  reviewCount: number;
  replyCount: number;
  myRatingCount: number;
}>;
