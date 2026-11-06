import { instance } from '@/services/core/http';

import type { GetPmfSurveyRequestDto, GetPmfSurveyResponseDto } from './dto';

export const getPmfSurvey =
  async ({}: GetPmfSurveyRequestDto): Promise<GetPmfSurveyResponseDto> => {
    const response = await instance('/api/v1/pmf-survey', {
      method: 'get',
    });

    return response.data;
  };
