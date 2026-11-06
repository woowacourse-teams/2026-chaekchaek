export interface PostPmfSurveyResponsesCommand {
  submissionToken: string;
  answers: { questionCode: string; answerCodes: string[] }[];
}

export type PostPmfSurveyResponses = (command: PostPmfSurveyResponsesCommand) => Promise<undefined>;
