import type { RequestContext } from '@/services/context/requestContext';

export interface PostPmfSurveyResponsesCommand {
  submissionToken: string;
  answers: { questionCode: string; answerCodes: string[] }[];
}

export type PostPmfSurveyResponses = (
  command: PostPmfSurveyResponsesCommand,
  context?: RequestContext,
) => Promise<undefined>;
