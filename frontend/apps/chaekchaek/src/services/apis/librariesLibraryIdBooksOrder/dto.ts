import type { ResponseDto } from '@/services/apis/api.types';

export interface PatchLibrariesLibraryIdBooksOrderRequestDto {
  pathParams: [{ name: 'libraryId'; value: number }];
  data: { bookIds: number[] };
}

export type PatchLibrariesLibraryIdBooksOrderResponseDto = ResponseDto<undefined>;
