import type { ResponseDto } from '@/services/apis/api.types';

export interface PatchLibraryBulkStatusRequestDto {
  data: { bookIds: number[]; status: string };
}

export type PatchLibraryBulkStatusResponseDto = ResponseDto<undefined>;
