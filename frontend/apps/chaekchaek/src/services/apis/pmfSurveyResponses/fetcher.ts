import { instance } from '@/services/core/http';

import type { PostPmfSurveyResponsesRequestDto, PostPmfSurveyResponsesResponseDto } from './dto';

export const postPmfSurveyResponses = async ({
  data: { submissionToken, answers },
}: PostPmfSurveyResponsesRequestDto): Promise<PostPmfSurveyResponsesResponseDto> => {
  const response = await instance('/api/v1/pmf-survey/responses', {
    method: 'post',
    data: { submissionToken, answers },
  });

  return response.data;
};
