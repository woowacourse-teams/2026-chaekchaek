import type { ResponseDto } from '@/services/apis/api.types';

export interface DeleteLibrariesLibraryIdRequestDto {
  pathParams: [{ name: 'libraryId'; value: number }];
}

export type DeleteLibrariesLibraryIdResponseDto = ResponseDto<undefined>;
export interface GetLibrariesLibraryIdRequestDto {
  pathParams: [{ name: 'libraryId'; value: number }];
  query: { page: number };
}

export type GetLibrariesLibraryIdResponseDto = ResponseDto<{
  libraryId: number;
  memberId: number;
  name: string;
  isDefault: boolean;
  totalCount: number;
  nextPage: number;
  items: {
    bookId: number;
    isbn13: string;
    title: string;
    spineImageUrl: string;
    coverImageUrl: string;
    totalPages: number;
    authors: string[];
  }[];
}>;
export interface PostLibrariesLibraryIdRequestDto {
  pathParams: [{ name: 'libraryId'; value: number }];
  data: { isbn13: string; totalPages: number };
}

export type PostLibrariesLibraryIdResponseDto = ResponseDto<undefined>;
