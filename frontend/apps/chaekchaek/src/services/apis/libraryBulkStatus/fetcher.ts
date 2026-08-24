import { instance } from '@/services/core/http';

import type { PatchLibraryBulkStatusRequestDto, PatchLibraryBulkStatusResponseDto } from './dto';

export const patchLibraryBulkStatus = async ({
  data: { bookIds, status },
}: PatchLibraryBulkStatusRequestDto): Promise<PatchLibraryBulkStatusResponseDto> => {
  const response = await instance('/api/v1/library/bulk-status', {
    method: 'patch',
    data: { bookIds, status },
  });

  return response.data;
};
