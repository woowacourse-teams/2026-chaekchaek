import { instance } from '@/services/core/http';
import { createFetcherRequestHeaders } from '@/services/context/requestHeaders';

import type { PostPmfSurveyResponsesRequestDto, PostPmfSurveyResponsesResponseDto } from './dto';

export const postPmfSurveyResponses = async ({
  data: { submissionToken, answers },
  headers: { 'X-Guest-Token': guestToken } = {},
}: PostPmfSurveyResponsesRequestDto): Promise<PostPmfSurveyResponsesResponseDto> => {
  const requestHeaders = createFetcherRequestHeaders({ 'X-Guest-Token': guestToken });

  const response = await instance('/api/v1/pmf-survey/responses', {
    method: 'post',
    data: { submissionToken, answers },
    headers: requestHeaders,
  });

  return response.data;
};
