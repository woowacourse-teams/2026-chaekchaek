import type { ResponseDto } from '@/services/apis/api.types';

export interface GetPmfSurveyRequestDto {}

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
