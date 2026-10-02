export type Book3DProps = {
  title?: string;
  coverImageUrl?: string;
  spineImageUrl?: string;
  backImageUrl?: string;
  /** Book height in pixels. Width and thickness follow the supplied image ratios. */
  height?: number;
  className?: string;
};
