import { requestAjax } from '@/services/core/http';

import type { GetLibraryRequestDto, GetLibraryResponseDto } from './dto';

export const getLibrary = async ({
  query: { page, status, sort },
}: GetLibraryRequestDto): Promise<GetLibraryResponseDto> => {
  const response = await requestAjax('/api/v1/library', {
    method: 'get',
    query: { page, status, sort },
  });

  return response.data;
};

import type { PostLibraryRequestDto, PostLibraryResponseDto } from './dto';

export const postLibrary = async ({
  data: { isbn13, totalPages, status },
}: PostLibraryRequestDto): Promise<PostLibraryResponseDto> => {
  const response = await requestAjax('/api/v1/library', {
    method: 'post',
    data: { isbn13, totalPages, status },
  });

  return response.data;
};
