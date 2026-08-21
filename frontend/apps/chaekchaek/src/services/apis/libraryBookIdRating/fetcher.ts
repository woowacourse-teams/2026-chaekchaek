import { requestAjax } from '@/services/core/http';

import type { PutLibraryBookIdRatingRequestDto, PutLibraryBookIdRatingResponseDto } from './dto';

export const putLibraryBookIdRating = async ({
  pathParams: [{ value: bookId }],
  data: { rating },
}: PutLibraryBookIdRatingRequestDto): Promise<PutLibraryBookIdRatingResponseDto> => {
  const response = await requestAjax(`/api/v1/library/${bookId}/rating`, {
    method: 'put',
    // pathParams: [{ name: 'bookId', value: bookId }],
    data: { rating },
  });

  return response.data;
};

import type {
  DeleteLibraryBookIdRatingRequestDto,
  DeleteLibraryBookIdRatingResponseDto,
} from './dto';

export const deleteLibraryBookIdRating = async ({
  pathParams: [{ value: bookId }],
}: DeleteLibraryBookIdRatingRequestDto): Promise<DeleteLibraryBookIdRatingResponseDto> => {
  const response = await requestAjax(`/api/v1/library/${bookId}/rating`, {
    method: 'delete',
    // pathParams: [{ name: 'bookId', value: bookId }],
  });

  return response.data;
};
