import { instance } from '@/services/core/http';

import type { DeleteLibraryBookIdRequestDto, DeleteLibraryBookIdResponseDto } from './dto';

export const deleteLibraryBookId = async ({
  pathParams: [{ value: bookId }],
}: DeleteLibraryBookIdRequestDto): Promise<DeleteLibraryBookIdResponseDto> => {
  const response = await instance('/api/v1/library', {
    method: 'delete',
    pathParams: [{ name: 'bookId', value: bookId }],
  });

  return response.data;
};
