import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
  title: '책췍 기술 블로그',
  tagline: '함께 읽고, 만들고, 기록하는 책췍의 기술 이야기',
  favicon: 'img/chaekchweck-icon.png',

  future: {
    v4: true,
  },

  url: 'https://woowacourse-teams.github.io',
  baseUrl: '/2026-chaekchaek/',
  trailingSlash: false,

  organizationName: 'woowacourse-teams',
  projectName: '2026-chaekchaek',

  onBrokenLinks: 'throw',

  i18n: {
    defaultLocale: 'ko',
    locales: ['ko'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl:
            'https://github.com/woowacourse-teams/2026-chaekchaek/edit/blog/team-blog/',
        },
        blog: {
          showReadingTime: true,
          blogTitle: '책췍 기술 블로그',
          blogDescription: '책췍 팀이 제품을 만들며 쌓은 기술적 고민과 배움을 기록합니다.',
          blogSidebarTitle: '최근 글',
          blogSidebarCount: 'ALL',
          editUrl:
            'https://github.com/woowacourse-teams/2026-chaekchaek/edit/blog/team-blog/',
          feedOptions: {
            type: ['rss', 'atom'],
            xslt: true,
          },
          onInlineTags: 'throw',
          onInlineAuthors: 'throw',
          onUntruncatedBlogPosts: 'throw',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  plugins: [require.resolve('./src/plugins/teamBlogHome')],

  themeConfig: {
    metadata: [
      {
        name: 'keywords',
        content: '책췍, ChaekChaek, 우아한테크코스, 기술 블로그',
      },
    ],
    colorMode: {
      defaultMode: 'light',
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: '책췍',
      logo: {
        alt: '책췍 로고',
        src: 'img/chaekchweck-icon.png',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: '문서',
        },
        {to: '/blog', label: '글 모아보기', position: 'left'},
        {to: '/blog/tags', label: '태그', position: 'left'},
        {to: '/blog/archive', label: '아카이브', position: 'left'},
        {
          href: 'https://github.com/woowacourse-teams/2026-chaekchaek',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: '책췍',
          items: [
            {label: '문서', to: '/docs/intro'},
            {label: '기술 블로그', to: '/blog'},
            {
              label: '프로젝트 저장소',
              href: 'https://github.com/woowacourse-teams/2026-chaekchaek',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} 책췍. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'javascript', 'kotlin', 'sql', 'yaml'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
