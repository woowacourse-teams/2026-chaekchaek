import { useState } from 'react';

import { Accordion, Surface, Title } from '@chaekchaek/design-system';

import type { Props } from './BookQuestion.types';

export const BookQuestion = ({ question, plot, extract }: Props) => {
  const items = [
    {
      text: '줄거리 보기',
      value: 'plot',
      panel: plot && <Surface>{plot}</Surface>,
    },
    {
      text: '본문 발췌 보기',
      value: 'extract',
      panel: extract && <Surface>{extract}</Surface>,
    },
  ].filter((item) => item.panel);

  const [value, setValue] = useState<(string | number)[]>([]);

  const handleChange = (value: (string | number)[]) => {
    setValue(value);
  };

  return (
    <>
      {question && (
        <>
          <Title level="main">여운 한 줄</Title>
          <Surface sx={{ mt: 4 }}>{question}</Surface>
        </>
      )}

      <Accordion sx={{ mt: 4, mb: 8 }} value={value} items={items} onChange={handleChange} />
    </>
  );
};
