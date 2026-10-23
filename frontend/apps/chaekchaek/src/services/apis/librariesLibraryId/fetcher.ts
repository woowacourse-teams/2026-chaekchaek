import { instance } from '@/services/core/http';

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

import type { GetLibrariesLibraryIdRequestDto, GetLibrariesLibraryIdResponseDto } from './dto';

export const getLibrariesLibraryId = async ({
  pathParams: [{ value: libraryId }],
  query: { page },
}: GetLibrariesLibraryIdRequestDto): Promise<GetLibrariesLibraryIdResponseDto> => {
  const response = await instance('/api/v1/libraries', {
    method: 'get',
    pathParams: [{ name: 'libraryId', value: libraryId }],
    query: { page },
  });

  return response.data;
};

import type { PostLibrariesLibraryIdRequestDto, PostLibrariesLibraryIdResponseDto } from './dto';

export const postLibrariesLibraryId = async ({
  pathParams: [{ value: libraryId }],
  data: { isbn13, totalPages },
}: PostLibrariesLibraryIdRequestDto): Promise<PostLibrariesLibraryIdResponseDto> => {
  const response = await instance('/api/v1/libraries', {
    method: 'post',
    pathParams: [{ name: 'libraryId', value: libraryId }],
    data: { isbn13, totalPages },
  });

  return response.data;
};
