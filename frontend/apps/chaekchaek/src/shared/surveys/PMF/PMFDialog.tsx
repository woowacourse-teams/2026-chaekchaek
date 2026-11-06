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

  const question = questions[currentIndex];

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
            return (
              <Radio sx={{ mt: 2 }} block size="large" id={question.questionCode}>
                {option.label}
              </Radio>
            );
          })}
        </Dialog.Body>
        <Dialog.Footer>
          <ButtonStack>
            <Button variant="ghost" onClick={handleClickPrev}>
              이전
            </Button>
            <Button variant="primary" onClick={handleClickNext}>
              다음
            </Button>
          </ButtonStack>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
