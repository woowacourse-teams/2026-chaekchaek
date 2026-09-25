import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { scrollbar } from './scrollbar';

const renderScrollbar = (theme: keyof typeof scrollbar) => {
  const isDark = theme === 'dark';

  return (
    <section
      style={{
        width: 360,
        maxWidth: '100%',
        padding: 24,
        borderRadius: 12,
        background: isDark ? '#090a0c' : '#fcfaf7',
        color: isDark ? '#ffffff' : '#25272c',
      }}
    >
      <h2 style={{ margin: '0 0 8px', fontSize: 20 }}>{isDark ? 'Dark' : 'Default'}</h2>
      <code style={{ fontSize: 13 }}>{`className={scrollbar.${theme}}`}</code>
      <p style={{ margin: '12px 0 20px', fontSize: 14 }}>아래 목록을 스크롤해 보세요.</p>

      <div
        className={scrollbar[theme]}
        role="region"
        aria-label={`${isDark ? '다크' : '기본'} 스크롤바 미리보기`}
        tabIndex={0}
        style={{ height: 280, overflowY: 'auto' }}
      >
        <ul style={{ margin: 0, padding: '0 16px 0 0', listStyle: 'none' }}>
          {Array.from({ length: 12 }, (_, index) => (
            <li
              key={index}
              style={{
                padding: '20px 0',
                borderBottom: `1px solid ${isDark ? '#2b2e35' : '#dedbd5'}`,
              }}
            >
              <strong style={{ fontSize: 14 }}>리뷰 {index + 1}</strong>
              <p style={{ margin: '8px 0 0', fontSize: 14, lineHeight: 1.6 }}>
                책을 읽으며 마음에 남은 생각을 기록합니다.
              </p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
};

const meta = {
  title: 'Styles/Scrollbar',
  parameters: {
    layout: 'centered',
  },
} satisfies Meta;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: () => renderScrollbar('default'),
};

export const Dark: Story = {
  render: () => renderScrollbar('dark'),
};

export const Comparison: Story = {
  render: () => (
    <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: 24 }}>
      {renderScrollbar('default')}
      {renderScrollbar('dark')}
    </div>
  ),
};
