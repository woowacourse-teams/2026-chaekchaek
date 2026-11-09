import type { RequestContext } from '@/services/context/requestContext';

export interface GetPmfSurveyParams {}

export type GetPmfSurvey = (
  params: GetPmfSurveyParams,
  context?: RequestContext,
) => Promise<{
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
