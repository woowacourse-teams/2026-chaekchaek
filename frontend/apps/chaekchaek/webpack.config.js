import path from 'node:path';
import { fileURLToPath } from 'node:url';

import webpack from 'webpack';
import HtmlWebpackPlugin from 'html-webpack-plugin';
import ReactRefreshWebpackPlugin from '@pmmmwh/react-refresh-webpack-plugin';
import CopyWebpackPlugin from 'copy-webpack-plugin';
import dotenv from 'dotenv';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

dotenv.config();

export default (_, argv) => {
  const isDevelopment = argv.mode === 'development';

  const env = Object.entries(process.env)
    .filter(([key]) => key.startsWith('APP_'))
    .reduce(
      (acc, [key, value]) => ({
        ...acc,
        [`process.env.${key}`]: JSON.stringify(value),
      }),
      {},
    );

  return {
    mode: 'development',
    entry: path.resolve(__dirname, 'src/main.tsx'),
    output: {
      path: path.resolve(__dirname, 'dist'),
      filename: 'main.js',
      publicPath: '/',
      assetModuleFilename: 'assets/[name].[contenthash][ext][query]',
      clean: true,
    },
    resolve: {
      extensions: ['.tsx', '.ts', '.jsx', '.js'],
      extensionAlias: {
        '.js': ['.ts', '.js'],
        '.jsx': ['.tsx', '.jsx'],
      },
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },
    module: {
      rules: [
        {
          test: /\.[jt]sx?$/,
          exclude: /node_modules/,
          use: {
            loader: 'swc-loader',
            options: {
              jsc: {
                parser: {
                  syntax: 'typescript',
                  tsx: true,
                },
                transform: {
                  react: {
                    runtime: 'automatic',
                    refresh: isDevelopment,
                  },
                },
              },
            },
          },
        },
        // CSS Modules
        {
          test: /\.module\.css$/i,

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
        },
        // 일반 CSS
        {
          test: /\.css$/i,
          exclude: /\.module\.css$/i,
          use: ['style-loader', 'css-loader'],
        },
        {
          test: /\.(png|jpe?g|gif|webp|avif)$/i,
          type: 'asset/resource',
        },
        {
          test: /\.svg$/i,
          resourceQuery: /component/,
          issuer: /\.[jt]sx?$/,
          use: [
            {
              loader: '@svgr/webpack',
              options: {
                svgoConfig: {
                  plugins: [
                    {
                      name: 'preset-default',
                      params: {
                        overrides: {
                          removeViewBox: false,
                        },
                      },
                    },
                  ],
                },
              },
            },
          ],
        },
        {
          test: /\.svg$/i,
          resourceQuery: { not: [/component/] },
          type: 'asset/resource',
        },
        {
          test: /\.(woff2?|eot|ttf|otf)$/i,
          type: 'asset/resource',
        },
      ],
    },
    plugins: [
      new HtmlWebpackPlugin({
        template: path.resolve(__dirname, 'public/index.html'),
      }),
      isDevelopment && new ReactRefreshWebpackPlugin(),
      new CopyWebpackPlugin({
        patterns: [
          {
            from: path.resolve(__dirname, 'public'),
            to: path.resolve(__dirname, 'dist'),
            noErrorOnMissing: true,
            globOptions: {
              ignore: ['**/index.html'],
            },
          },
        ],
      }),
      new webpack.DefinePlugin({
        __DEV__: JSON.stringify(isDevelopment),
        ...env,
      }),
    ].filter(Boolean),
    devServer: {
      port: 3000,
      open: true,
      hot: true,
      historyApiFallback: true,
    },
  };
};
