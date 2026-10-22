import { instance } from '@/services/core/http';

import type { PostLibrariesRequestDto, PostLibrariesResponseDto } from './dto';

export const postLibraries = async ({
  data: { name },
}: PostLibrariesRequestDto): Promise<PostLibrariesResponseDto> => {
  const response = await instance('/api/v1/libraries', {
    method: 'post',
    data: { name },
  });

  return response.data;
};

import type { GetLibrariesRequestDto, GetLibrariesResponseDto } from './dto';

export const getLibraries = async ({
  query: { memberId },
}: GetLibrariesRequestDto): Promise<GetLibrariesResponseDto> => {
  const response = await instance('/api/v1/libraries', {
    method: 'get',
    query: { memberId },
  });

  return response.data;
};
