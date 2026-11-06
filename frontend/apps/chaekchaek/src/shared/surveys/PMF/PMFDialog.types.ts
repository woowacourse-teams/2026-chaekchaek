type Option = { answerCode: string; label: string };

type Question = {
  questionCode: string;
  question: string;
  multipleSelection: boolean;
  options: Option[];
};

export type Props = {
  questions: Question[];
  submissionToken: string;
  onClose: () => void;
};
