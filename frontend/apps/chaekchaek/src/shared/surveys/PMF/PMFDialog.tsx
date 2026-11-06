import { Button, ButtonStack, Dialog, Radio, Checkbox, Title } from '@chaekchaek/design-system';

import type { Props } from './PMFDialog.types';
import { useState, type ChangeEvent } from 'react';

export const PMFDialog = (props: Props) => {
  const { questions, submissionToken, onClose } = props;

  const [currentIndex, setCurrentIndex] = useState(0);
  const handleClickPrev = () => {
    if (currentIndex === 0) return;
    setCurrentIndex((prev) => prev - 1);
  };
  const handleClickNext = () => {
    if (currentIndex === questions.length - 1) return;
    setCurrentIndex((prev) => prev + 1);
  };

  const [answers, setAnswers] = useState<string[][]>(() =>
    Array.from({ length: questions.length }).map(() => []),
  );
  const changeAnswer = (currentIndex: number, currentAnswer: string[]) => {
    setAnswers(
      answers.map((answer, answerIndex) => (answerIndex === currentIndex ? currentAnswer : answer)),
    );
  };

  const question = questions[currentIndex];
  const answer = answers[currentIndex];

  const isFirstQuestion = currentIndex === 0;
  const hasNextQuestion = currentIndex !== answers.length - 1;
  const isLastQuestion = currentIndex === answers.length - 1;

  const hasNOAnswer = !(answer && answer.length > 0);

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
            const isChecked = !!answer?.includes(option.answerCode);
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
                    changeAnswer(currentIndex, [e.target.value]);
                  }}
                >
                  {option.label}
                </Radio>
              );
            return (
              answer && (
                <Checkbox
                  sx={{ mt: 2 }}
                  block
                  size="large"
                  id={question.questionCode}
                  value={option.answerCode}
                  checked={isChecked}
                  onChange={(e: ChangeEvent<HTMLInputElement>) => {
                    const value = e.target.value;
                    const isIncluded = answer?.includes(value);
                    const newAnswer = isIncluded
                      ? answer?.filter((a) => a !== value)
                      : [...answer, value];

                    changeAnswer(currentIndex, newAnswer);
                  }}
                >
                  {option.label}
                </Checkbox>
              )
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
              <Button variant="primary" onClick={() => {}}>
                제출
              </Button>
            )}
          </ButtonStack>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
