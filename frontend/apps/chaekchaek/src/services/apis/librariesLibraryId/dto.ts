import type { ResponseDto } from '@/services/apis/api.types';

export interface GetLibrariesLibraryIdRequestDto {
  pathParams: [{ name: 'libraryId'; value: number }];
}

export type GetLibrariesLibraryIdResponseDto = ResponseDto<{
  isDefault: boolean;
  libraryId: number;
  name: string;
  totalCount: number;
  maxSize: number;
  items: {
    coverImageUrl: string;
    isbn13: string;
    spineImageUrl: string | null;
    title: string;
    authors: string[];
    bookId: number;
  }[];
  memberId: number;
}>;
export interface PostLibrariesLibraryIdRequestDto {
  pathParams: [{ name: 'libraryId'; value: number }];
  data: { isbn13: string };
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
