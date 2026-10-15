import type { CSSProperties } from 'react';

export type ShelfBook = {
  title: string;
  author: string;
  color: string;
  ink: string;
  height: number;
  width: number;
  spineWidth: number;
  tilt: number;
  favorite?: boolean;
  cover?: string;
};

export type BookLayout = {
  x: number;
  rotateY: number;
  rotateZ: number;
  origin: 'left' | 'right';
  markerX: number;
};

export type BookStyle = CSSProperties & {
  '--book-x': string;
  '--book-y-rotation': string;
  '--book-z-rotation': string;
  '--book-origin': string;
  '--book-spine-width': string;
  '--book-color': string;
  '--book-ink': string;
  '--book-delay': string;
  '--book-duration': string;
  '--book-marker-x': string;
};
