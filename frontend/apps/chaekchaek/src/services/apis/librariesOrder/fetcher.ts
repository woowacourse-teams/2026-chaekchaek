import { instance } from '@/services/core/http';

import type { PatchLibrariesOrderRequestDto, PatchLibrariesOrderResponseDto } from './dto';

export const patchLibrariesOrder = async ({
  data: { libraryIds },
}: PatchLibrariesOrderRequestDto): Promise<PatchLibrariesOrderResponseDto> => {
  const response = await instance('/api/v1/libraries/order', {
    method: 'patch',
    data: { libraryIds },
  });

  return response.data;
};
