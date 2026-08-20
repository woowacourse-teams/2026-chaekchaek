import { requestAjax } from '@/services/core/http';

import type { DeleteLibraryBookIdRequestDto, DeleteLibraryBookIdResponseDto } from './dto';

export const deleteLibraryBookId = async ({
  pathParams: [{ value: bookId }],
}: DeleteLibraryBookIdRequestDto): Promise<DeleteLibraryBookIdResponseDto> => {
  const response = await requestAjax('/api/v1/libraryBookId', {
    method: 'delete',
    pathParams: [{ name: 'bookId', value: bookId }],
  });

  return response.data;
};

import type { PatchLibraryBookIdRequestDto, PatchLibraryBookIdResponseDto } from './dto';

export const patchLibraryBookId = async ({
  pathParams: [{ value: bookId }],
  data: { status, currentPage },
}: PatchLibraryBookIdRequestDto): Promise<PatchLibraryBookIdResponseDto> => {
  const response = await requestAjax('/api/v1/libraryBookId', {
    method: 'patch',
    pathParams: [{ name: 'bookId', value: bookId }],
    data: { status, currentPage },
  });

  return response.data;
};
