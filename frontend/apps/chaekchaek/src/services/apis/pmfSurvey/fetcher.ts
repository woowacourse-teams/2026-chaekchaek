import { instance } from '@/services/core/http';
import { createFetcherRequestHeaders } from '@/services/context/requestHeaders';

import type { GetPmfSurveyRequestDto, GetPmfSurveyResponseDto } from './dto';

export const getPmfSurvey = async ({
  headers: { 'X-Guest-Token': guestToken } = {},
}: GetPmfSurveyRequestDto): Promise<GetPmfSurveyResponseDto> => {
  const requestHeaders = createFetcherRequestHeaders({ 'X-Guest-Token': guestToken });
  const response = await instance('/api/v1/pmf-survey', {
    method: 'get',
    headers: requestHeaders,
  });

  return response.data;
};
