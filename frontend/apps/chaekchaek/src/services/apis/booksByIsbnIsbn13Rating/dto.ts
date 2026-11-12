import type { ResponseDto } from '@/services/apis/api.types';

export interface PutBooksByIsbnIsbn13RatingRequestDto {
  pathParams: [{ name: 'isbn13'; value: string }];
  data: { rating: number };
}

export type PutBooksByIsbnIsbn13RatingResponseDto = ResponseDto<{
  addedAt: string;
  coverImageUrl: string;
  rating: number;
  title: string;
  commentCount: number;
  bookId: number;
  translators: string[];
  isbn13: string;
  publisher: string;
  spineImageUrl: string;
  publishedDate: string;
  category: string;
  backImageUrl: string;
  authors: string[];
}>;
export interface DeleteBooksByIsbnIsbn13RatingRequestDto {
  pathParams: [{ name: 'isbn13'; value: string }];
}

export type DeleteBooksByIsbnIsbn13RatingResponseDto = ResponseDto<undefined>;
