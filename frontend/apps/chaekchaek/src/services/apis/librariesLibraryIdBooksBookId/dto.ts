import type { ResponseDto } from '@/services/apis/api.types';

export interface DeleteLibrariesLibraryIdBooksBookIdRequestDto {
  pathParams: [{ name: 'libraryId'; value: number }, { name: 'bookId'; value: number }];
}

export type DeleteLibrariesLibraryIdBooksBookIdResponseDto = ResponseDto<undefined>;
