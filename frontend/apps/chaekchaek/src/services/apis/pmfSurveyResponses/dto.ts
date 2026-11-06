import type { ResponseDto } from '@/services/apis/api.types';

export interface PostPmfSurveyResponsesRequestDto {
  data: { submissionToken: string; answers: { questionCode: string; answerCodes: string[] }[] };
}

export type PostPmfSurveyResponsesResponseDto = ResponseDto<undefined>;
