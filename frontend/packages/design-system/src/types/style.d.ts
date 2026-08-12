declare module '*.module.css' {
  const styles: Record<string, string>;
  export default styles;
}

declare module '*.png' {
  const src: string;

  export default src;
}

declare module '*.jpg' {
  const src: string;

  export default src;
}

declare module '*.svg' {
  const src: string;

  export default src;
}
