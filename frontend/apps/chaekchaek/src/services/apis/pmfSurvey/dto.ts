import type { ResponseDto } from '@/services/apis/api.types';
import type { RequestHeaders } from '@/services/context/requestHeaders';

export interface GetPmfSurveyRequestDto {
  headers?: RequestHeaders;
}

export type GetPmfSurveyResponseDto = ResponseDto<{
  alreadySubmitted: boolean;
  eligible: boolean;
  submissionToken: string;
  questions: {
    question: string;
    questionCode: string;
    options: {
      answerCode: string;
      label: string;
    }[];
    multipleSelection: boolean;
  }[];
}>;
