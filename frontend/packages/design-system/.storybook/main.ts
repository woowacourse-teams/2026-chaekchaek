import type { StorybookConfig } from '@storybook/react-webpack5';

import { dirname } from 'path';

import { fileURLToPath } from 'url';

/**
 * This function is used to resolve the absolute path of a package.
 * It is needed in projects that use Yarn PnP or are set up within a monorepo.
 */
function getAbsolutePath(value: string) {
  return dirname(fileURLToPath(import.meta.resolve(`${value}/package.json`)));
}
const config: StorybookConfig = {
  stories: ['../src/**/*.stories.@(js|jsx|mjs|ts|tsx)'],
  addons: [getAbsolutePath('@storybook/addon-webpack5-compiler-swc')],
  framework: getAbsolutePath('@storybook/react-webpack5'),
  typescript: {
    check: true,
  },
  webpackFinal: async (config) => {
    config.resolve ??= {};
    config.resolve.extensionAlias = {
      ...config.resolve.extensionAlias,
      '.js': ['.ts', '.tsx', '.js'],
      '.jsx': ['.tsx', '.jsx'],
    };

    const cssModulePattern = /\.module\.css$/i;

    // Storybook의 기본 CSS 규칙과 아래 CSS Modules 규칙이 동시에 적용되지
    // 않도록 기본 규칙에서는 *.module.css를 제외한다.
    config.module?.rules?.forEach((rule) => {
      if (
        rule &&
        typeof rule === 'object' &&
        rule.test instanceof RegExp &&
        rule.test.test('styles.css')
      ) {
        rule.exclude = rule.exclude ? [rule.exclude, cssModulePattern] : cssModulePattern;
      }
    });

    config.module?.rules?.push({
      test: cssModulePattern,
      use: [
        'style-loader',
        {
          loader: 'css-loader',
          options: {
            modules: {
              namedExport: false,
              exportLocalsConvention: 'as-is',
              localIdentName: '[name]__[local]--[hash:base64:5]',
            },
          },
        },
      ],
    });

    if (config.module?.rules) {
      config.module.rules = config.module?.rules?.map((rule) => {
        if (
          typeof rule === 'object' &&
          rule !== null &&
          rule.test instanceof RegExp &&
          rule.test.test('.svg')
        ) {
          return {
            ...rule,
            exclude: /\.svg$/i,
          };
        }

        return rule;
      });

      config.module?.rules?.push(
        {
          test: /\.svg$/i,
          resourceQuery: /component/,
          issuer: /\.[jt]sx?$/,
          use: ['@svgr/webpack'],
        },
        {
          test: /\.svg$/i,
          resourceQuery: { not: [/component/] },
          type: 'asset/resource',
        },
      );
    }

    return config;
  },
};
export default config;
