import type { ResponseDto } from '@/services/apis/api.types';

export interface PatchLibrariesOrderRequestDto {
  data: { libraryIds: number[] };
}

export type PatchLibrariesOrderResponseDto = ResponseDto<undefined>;
