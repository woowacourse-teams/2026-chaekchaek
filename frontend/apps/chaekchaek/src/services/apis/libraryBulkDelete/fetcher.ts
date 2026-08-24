import { instance } from '@/services/core/http';

import type { PostLibraryBulkDeleteRequestDto, PostLibraryBulkDeleteResponseDto } from './dto';

export const postLibraryBulkDelete = async ({
  data: { bookIds },
}: PostLibraryBulkDeleteRequestDto): Promise<PostLibraryBulkDeleteResponseDto> => {
  const response = await instance('/api/v1/library/bulk-delete', {
    method: 'post',
    data: { bookIds },
  });

  return response.data;
};
