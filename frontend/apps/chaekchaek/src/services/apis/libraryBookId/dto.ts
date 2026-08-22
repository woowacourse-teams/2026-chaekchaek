import type { ResponseDto } from '@/services/apis/api.types';

export interface DeleteLibraryBookIdRequestDto {
  pathParams: [{ name: 'bookId'; value: number }];
}

export type DeleteLibraryBookIdResponseDto = ResponseDto<undefined>;
export interface PatchLibraryBookIdRequestDto {
  pathParams: [{ name: 'bookId'; value: number }];
  data: { status?: string | undefined; currentPage?: number | undefined };
}

export type PatchLibraryBookIdResponseDto = ResponseDto<{
  bookId: number;
  isbn13: string;
  title: string;
  coverImageUrl: string;
  authors: string[];
  translators: string[];
  publisher: string;
  category: string;
  publishedDate: string;
  totalPages: number;
  commentCount: number;
  status: string;
  currentPage: number;
  rating: number;
  addedAt: string;
  readingUpdatedAt: string;
}>;
