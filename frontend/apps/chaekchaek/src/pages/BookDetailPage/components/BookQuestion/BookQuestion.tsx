import { useState } from 'react';

import { Accordion, Surface, Title } from '@chaekchaek/design-system';

import type { Props } from './BookQuestion.types';

const titles = {
  question: '여운 한 줄',
  plot: '줄거리 보기',
  extract: '본문 발췌 보기',
};

export const BookQuestion = ({ question, plot, extract }: Props) => {
  const items = [
    {
      text: titles.plot,
      value: 'plot',
      panel: plot && <Surface>{plot}</Surface>,
    },
    {
      text: titles.extract,
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
          <Title level="main">{titles.question}</Title>
          <Surface sx={{ mt: 4 }}>{question}</Surface>
        </>
      )}

      {items.length > 1 ? (
        <Accordion sx={{ mt: 4, mb: 8 }} value={value} items={items} onChange={handleChange} />
      ) : (
        <>
          {plot && (
            <>
              <Title level="main">{titles.plot}</Title>
              <Surface sx={{ mt: 4, mb: 8 }}>{plot}</Surface>
            </>
          )}
          {extract && (
            <>
              <Title level="main">{titles.extract}</Title>
              <Surface sx={{ mt: 4, mb: 8 }}>{extract}</Surface>
            </>
          )}
        </>
      )}
    </>
  );
};
