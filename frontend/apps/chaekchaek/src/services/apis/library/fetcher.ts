import { instance } from '@/services/core/http';

import type { GetLibraryRequestDto, GetLibraryResponseDto } from './dto';

export const getLibrary = async ({
  query: { page, sort },
}: GetLibraryRequestDto): Promise<GetLibraryResponseDto> => {
  const response = await instance('/api/v1/library', {
    method: 'get',
    query: { page, sort },
  });

  return response.data;
};

import type { PostLibraryRequestDto, PostLibraryResponseDto } from './dto';

export const postLibrary = async ({
  data: { isbn13 },
}: PostLibraryRequestDto): Promise<PostLibraryResponseDto> => {
  const response = await instance('/api/v1/library', {
    method: 'post',
    data: { isbn13 },
  });

  return response.data;
};
