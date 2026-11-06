export interface GetPmfSurveyParams {}

export type GetPmfSurvey = (params: GetPmfSurveyParams) => Promise<{
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
