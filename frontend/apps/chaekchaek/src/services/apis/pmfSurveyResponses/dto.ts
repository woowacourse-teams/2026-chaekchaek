import type { ResponseDto } from '@/services/apis/api.types';
import type { RequestHeaders } from '@/services/context/requestHeaders';
export interface PostPmfSurveyResponsesRequestDto {
  headers?: RequestHeaders;
  data: {
    submissionToken: string;
    answers: { questionCode: string; answerCodes: string[] }[];
  };
}

export type PostPmfSurveyResponsesResponseDto = ResponseDto<undefined>;
