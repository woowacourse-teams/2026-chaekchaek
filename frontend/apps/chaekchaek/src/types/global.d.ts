declare module '*.json' {
  const value: any;
  export default value;
}

declare module '*.css' {
  const styles: Record<string, string>;
  export default styles;
}
