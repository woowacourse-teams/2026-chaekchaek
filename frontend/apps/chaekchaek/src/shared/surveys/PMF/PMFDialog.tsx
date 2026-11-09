import { Button, ButtonStack, Dialog, Radio, Checkbox, Title } from '@chaekchaek/design-system';

import type { Props } from './PMFDialog.types';
import { useState, type ChangeEvent } from 'react';

import { postPmfSurveyResponses } from '@/services/apis/pmfSurveyResponses/repository';
import { useExecute } from '@/services/core/useExecute';
import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

export const PMFDialog = (props: Props) => {
  const { questions, submissionToken, onSurveySubmitted, onClose } = props;

  const { guest } = useAuthContext();

  const [currentIndex, setCurrentIndex] = useState(0);
  const handleClickPrev = () => {
    if (currentIndex === 0) return;
    setCurrentIndex((prev) => prev - 1);
  };
  const handleClickNext = () => {
    if (currentIndex === questions.length - 1) return;
    setCurrentIndex((prev) => prev + 1);
  };

  const [answers, setAnswers] = useState<{ questionCode: string; answerCodes: string[] }[]>(() =>
    questions.map((question) => ({ questionCode: question.questionCode, answerCodes: [] })),
  );
  const changeAnswer = (questionCode: string, answerCodes: string[]) => {
    setAnswers(
      answers.map((answer) =>
        answer.questionCode === questionCode ? { ...answer, answerCodes } : answer,
      ),
    );
  };

  const question = questions[currentIndex];
  const answer = answers[currentIndex];

  const isFirstQuestion = currentIndex === 0;
  const hasNextQuestion = currentIndex !== answers.length - 1;
  const isLastQuestion = currentIndex === answers.length - 1;

  const hasNOAnswer = !(answer && answer.answerCodes.length > 0);

  const { mutate: postPmfSurveyMutate } = useExecute({
    executeFn: postPmfSurveyResponses,
  });

  const handleSubmitSurvey = async () => {
    await postPmfSurveyMutate(
      {
        submissionToken,
        answers,
      },
      guest?.guestToken
        ? {
            guestToken: guest?.guestToken,
          }
        : undefined,
    );

    await onSurveySubmitted();

    onClose();
  };

  return (
    <Dialog size="large" onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header>
          책췍 사용자 설문 ({currentIndex + 1}/ {questions.length})
        </Dialog.Header>
        <Dialog.Body>
          <Title sx={{ mb: 4 }} level="main" subtitle="하나만 선택해 주세요">
            {question?.question}
          </Title>

          {question?.options.map((option) => {
            if (!answer) return;
            const isChecked = !!answer.answerCodes.includes(option.answerCode);

            if (!question.multipleSelection)
              return (
                <Radio
                  sx={{ mt: 2 }}
                  block
                  size="large"
                  id={question.questionCode}
                  value={option.answerCode}
                  checked={isChecked}
                  onChange={(e: ChangeEvent<HTMLInputElement>) => {
                    changeAnswer(question.questionCode, [e.target.value]);
                  }}
                >
                  {option.label}
                </Radio>
              );
            return (
              <Checkbox
                sx={{ mt: 2 }}
                block
                size="large"
                id={question.questionCode}
                value={option.answerCode}
                checked={isChecked}
                onChange={(e: ChangeEvent<HTMLInputElement>) => {
                  const value = e.target.value;

                  const NONE = 'NO_UNKNOWN_FEATURES';

                  const getNextAnswers = (selected: string[], clicked: string) => {
                    // '없음'을 선택하면 다른 선택은 모두 해제
                    if (clicked === NONE) {
                      return [NONE];
                    }

                    // 일반 항목을 선택하면 '없음'은 해제
                    const selectedFeatures = selected.filter((code) => code !== NONE);

                    // 이미 선택한 항목이면 해제, 아니면 추가
                    return selectedFeatures.includes(clicked)
                      ? selectedFeatures.filter((code) => code !== clicked)
                      : [...selectedFeatures, clicked];
                  };

                  const nextAnswers = getNextAnswers(answer.answerCodes, value);
                  changeAnswer(answer.questionCode, nextAnswers);
                }}
              >
                {option.label}
              </Checkbox>
            );
          })}
        </Dialog.Body>
        <Dialog.Footer>
          <ButtonStack>
            {!isFirstQuestion && (
              <Button variant="ghost" onClick={handleClickPrev}>
                이전
              </Button>
            )}
            {hasNextQuestion && (
              <Button variant="primary" disabled={hasNOAnswer} onClick={handleClickNext}>
                다음
              </Button>
            )}
            {isLastQuestion && (
              <Button variant="primary" onClick={handleSubmitSurvey}>
                제출
              </Button>
            )}
          </ButtonStack>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
