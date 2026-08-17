declare const __DEV__: boolean;

declare const process: {
  env: NodeJS.ProcessEnv;
};

declare module '*.css' {
  const classes: { readonly [key: string]: string };
  export default classes;
}
