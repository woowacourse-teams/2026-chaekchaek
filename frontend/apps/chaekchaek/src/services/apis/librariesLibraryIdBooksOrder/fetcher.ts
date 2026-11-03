import { instance } from '@/services/core/http';

import type {
  PatchLibrariesLibraryIdBooksOrderRequestDto,
  PatchLibrariesLibraryIdBooksOrderResponseDto,
} from './dto';

export const patchLibrariesLibraryIdBooksOrder = async ({
  pathParams: [{ value: libraryId }],
  data: { bookIds },
}: PatchLibrariesLibraryIdBooksOrderRequestDto): Promise<PatchLibrariesLibraryIdBooksOrderResponseDto> => {
  const response = await instance(`/api/v1/libraries/${libraryId}/books/order`, {
    method: 'patch',
    data: { bookIds },
  });

  return response.data;
};
