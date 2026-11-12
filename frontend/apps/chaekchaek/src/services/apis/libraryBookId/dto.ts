import type { ResponseDto } from '@/services/apis/api.types';

export interface DeleteLibraryBookIdRequestDto {
  pathParams: [{ name: 'bookId'; value: number }];
}

export type DeleteLibraryBookIdResponseDto = ResponseDto<undefined>;
