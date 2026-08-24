import type { ResponseDto } from '@/services/apis/api.types';

export interface PostLibraryBulkDeleteRequestDto {
  data: { bookIds: number[] };
}

export type PostLibraryBulkDeleteResponseDto = ResponseDto<undefined>;
