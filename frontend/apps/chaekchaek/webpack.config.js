const path = require('node:path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ReactRefreshWebpackPlugin = require('@pmmmwh/react-refresh-webpack-plugin');

module.exports = (_, argv) => {
  const isDevelopment = argv.mode === 'development';

  return {
    mode: 'development',
    entry: path.resolve(__dirname, 'src/main.tsx'),
    output: {
      path: path.resolve(__dirname, 'dist'),
      filename: 'main.js',
      assetModuleFilename: 'assets/[name].[contenthash][ext][query]',
      clean: true,
    },
    resolve: {
      extensions: ['.tsx', '.ts', '.jsx', '.js'],
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
                modules: true,
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
    ].filter(Boolean),
    devServer: {
      port: 3000,
      open: true,
      hot: true,
      historyApiFallback: true,
    },
  };
};
