import { instance } from '@/services/core/http';

import type {
  DeleteLibrariesLibraryIdBooksBookIdRequestDto,
  DeleteLibrariesLibraryIdBooksBookIdResponseDto,
} from './dto';

export const deleteLibrariesLibraryIdBooksBookId = async ({
  pathParams: [{ value: libraryId }, { value: bookId }],
}: DeleteLibrariesLibraryIdBooksBookIdRequestDto): Promise<DeleteLibrariesLibraryIdBooksBookIdResponseDto> => {
  const response = await instance(`/api/v1/libraries/${libraryId}/books`, {
    method: 'delete',
    pathParams: [{ name: 'bookId', value: bookId }],
  });

  return response.data;
};
