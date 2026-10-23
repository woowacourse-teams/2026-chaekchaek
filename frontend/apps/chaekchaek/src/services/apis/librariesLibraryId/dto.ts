import type { ResponseDto } from '@/services/apis/api.types';

export interface GetLibrariesLibraryIdRequestDto {
  pathParams: [{ name: 'libraryId'; value: number }];
}

export type GetLibrariesLibraryIdResponseDto = ResponseDto<{
  isDefault: boolean;
  libraryId: number;
  name: string;
  totalCount: number;
  items: {
    coverImageUrl: string;
    isbn13: string;
    totalPages: number;
    spineImageUrl: string;
    currentPage: number;
    title: string;
    readingUpdatedAt: string;
    status: string;
    authors: string[];
    bookId: number;
  }[];
  memberId: number;
}>;
export interface PostLibrariesLibraryIdRequestDto {
  pathParams: [{ name: 'libraryId'; value: number }];
  data: { isbn13: string; totalPages: number; status: string };
}

export type PostLibrariesLibraryIdResponseDto = ResponseDto<undefined>;
export interface DeleteLibrariesLibraryIdRequestDto {
  pathParams: [{ name: 'libraryId'; value: number }];
}

export type DeleteLibrariesLibraryIdResponseDto = ResponseDto<undefined>;
export interface PatchLibrariesLibraryIdRequestDto {
  pathParams: [{ name: 'libraryId'; value: number }];
  data: { name: string };
}

export type PatchLibrariesLibraryIdResponseDto = ResponseDto<undefined>;
