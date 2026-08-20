import type { ResponseDto } from '@/services/apis/api.types';

export interface PutLibraryBookIdRatingRequestDto {
  pathParams: [{ name: 'bookId'; value: number }];
  data: { rating: number };
}

export type PutLibraryBookIdRatingResponseDto = ResponseDto<{
  addedAt: string;
  coverImageUrl: string;
  rating: number;
  title: string;
  readingUpdatedAt: string;
  commentCount: number;
  bookId: number;
  translators: string[];
  isbn13: string;
  totalPages: number;
  publisher: string;
  publishedDate: string;
  currentPage: number;
  category: string;
  status: string;
  authors: string[];
}>;
export interface DeleteLibraryBookIdRatingRequestDto {
  pathParams: [{ name: 'bookId'; value: number }];
}

export type DeleteLibraryBookIdRatingResponseDto = ResponseDto<undefined>;
