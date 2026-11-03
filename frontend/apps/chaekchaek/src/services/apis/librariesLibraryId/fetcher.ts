import { instance } from '@/services/core/http';

import type { GetLibrariesLibraryIdRequestDto, GetLibrariesLibraryIdResponseDto } from './dto';

export const getLibrariesLibraryId = async ({
  pathParams: [{ value: libraryId }],
}: GetLibrariesLibraryIdRequestDto): Promise<GetLibrariesLibraryIdResponseDto> => {
  const response = await instance('/api/v1/libraries', {
    method: 'get',
    pathParams: [{ name: 'libraryId', value: libraryId }],
  });

  return response.data;
};

import type { PostLibrariesLibraryIdRequestDto, PostLibrariesLibraryIdResponseDto } from './dto';

export const postLibrariesLibraryId = async ({
  pathParams: [{ value: libraryId }],
  data: { isbn13, totalPages, status },
}: PostLibrariesLibraryIdRequestDto): Promise<PostLibrariesLibraryIdResponseDto> => {
  const response = await instance('/api/v1/libraries', {
    method: 'post',
    pathParams: [{ name: 'libraryId', value: libraryId }],
    data: { isbn13, totalPages, status },
  });

  return response.data;
};

import type {
  DeleteLibrariesLibraryIdRequestDto,
  DeleteLibrariesLibraryIdResponseDto,
} from './dto';

export const deleteLibrariesLibraryId = async ({
  pathParams: [{ value: libraryId }],
}: DeleteLibrariesLibraryIdRequestDto): Promise<DeleteLibrariesLibraryIdResponseDto> => {
  const response = await instance('/api/v1/libraries', {
    method: 'delete',
    pathParams: [{ name: 'libraryId', value: libraryId }],
  });

  return response.data;
};

import type { PatchLibrariesLibraryIdRequestDto, PatchLibrariesLibraryIdResponseDto } from './dto';

export const patchLibrariesLibraryId = async ({
  pathParams: [{ value: libraryId }],
  data: { name },
}: PatchLibrariesLibraryIdRequestDto): Promise<PatchLibrariesLibraryIdResponseDto> => {
  const response = await instance('/api/v1/libraries', {
    method: 'patch',
    pathParams: [{ name: 'libraryId', value: libraryId }],
    data: { name },
  });

  return response.data;
};
