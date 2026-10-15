import { landingImages } from '../../imgs';

import type { BookLayout, ShelfBook } from './HeroBookshelf.types';

export const shelfBooks: readonly ShelfBook[] = [
  {
    title: '아몬드',
    author: '손원평',
    color: '#f0afbd',
    ink: '#422b32',
    height: 310,
    width: 200,
    spineWidth: 56,
    tilt: -3.4,
    favorite: true,
  },
  {
    title: '소년이 온다',
    author: '한강',
    color: '#49614b',
    ink: '#fff8eb',
    height: 354,
    width: 220,
    spineWidth: 36,
    tilt: -7,
  },
  {
    title: '불편한 편의점',
    author: '김호연',
    color: '#e96936',
    ink: '#fff8eb',
    height: 402,
    width: 250,
    spineWidth: 68,
    tilt: 0,
  },
  {
    title: '물고기는 존재하지 않는다',
    author: '룰루 밀러',
    color: '#293b35',
    ink: '#fff8eb',
    height: 345,
    width: 225,
    spineWidth: 48,
    tilt: -1.5,
    favorite: true,
  },
  {
    title: '사랑의 기술',
    author: '에리히 프롬',
    color: '#d8a3bc',
    ink: '#412637',
    height: 430,
    width: 265,
    spineWidth: 63,
    tilt: -3,
  },
  {
    title: '작별하지 않는다',
    author: '한강',
    color: '#244b52',
    ink: '#fff8eb',
    height: 425,
    width: 250,
    spineWidth: 48,
    tilt: -1.2,
    favorite: true,
  },
  {
    title: '이방인',
    author: '알베르 카뮈',
    color: '#32769a',
    ink: '#fff',
    height: 410,
    width: 263,
    spineWidth: 28,
    tilt: 0,
    cover: landingImages.stranger,
  },
  {
    title: '밝은 밤',
    author: '최은영',
    color: '#b97986',
    ink: '#fff8eb',
    height: 360,
    width: 220,
    spineWidth: 44,
    tilt: 1.8,
    favorite: true,
  },
  {
    title: '참을 수 없는 존재의 가벼움',
    author: '밀란 쿤데라',
    color: '#de5941',
    ink: '#fff8eb',
    height: 445,
    width: 275,
    spineWidth: 40,
    tilt: 2.2,
  },
  {
    title: '모순',
    author: '양귀자',
    color: '#346ba3',
    ink: '#fff8eb',
    height: 388,
    width: 240,
    spineWidth: 65,
    tilt: -2,
  },
  {
    title: '채식주의자',
    author: '한강',
    color: '#eeeade',
    ink: '#35352e',
    height: 350,
    width: 218,
    spineWidth: 34,
    tilt: 0,
    favorite: true,
  },
  {
    title: '데미안',
    author: '헤르만 헤세',
    color: '#202541',
    ink: '#fff8eb',
    height: 425,
    width: 262,
    spineWidth: 48,
    tilt: 1.5,
  },
  {
    title: '긴긴밤',
    author: '루리',
    color: '#e97c32',
    ink: '#fff8eb',
    height: 325,
    width: 210,
    spineWidth: 58,
    tilt: 0,
  },
];

export const INITIAL_INDEX = 6;
export const SHELF_BUFFER = 12;
export const TRANSITION_DURATION = { forward: 980, reverse: 1200 } as const;
export const AUTOPLAY_SWEEP = { minIndex: INITIAL_INDEX - 4, maxIndex: INITIAL_INDEX + 4 } as const;
export const AUTOPLAY_TIMING = {
  initialDelay: 1050,
  betweenBooks: 90,
  turnaroundHold: 2000,
} as const;
const CENTER_SLOT_WIDTH = 280;

export const getTransitionDuration = (direction: number) =>
  direction >= 0 ? TRANSITION_DURATION.forward : TRANSITION_DURATION.reverse;

export const getBook = (index: number): ShelfBook =>
  shelfBooks[((index % shelfBooks.length) + shelfBooks.length) % shelfBooks.length]!;

const getAngle = (slot: number) => (slot < 0 ? 1 : -1) * (Math.abs(slot) === 1 ? 77 : 89);

const getProjectedWidth = (book: ShelfBook, slot: number) => {
  const angle = (Math.abs(getAngle(slot)) * Math.PI) / 180;
  return book.width * Math.cos(angle) + book.spineWidth * Math.sin(angle);
};

// Each book occupies its own slot; only the central slot exposes the whole cover.
export const getBookLayout = (
  index: number,
  activeIndex: number,
  direction: number,
): BookLayout => {
  const slot = index - activeIndex;
  const book = getBook(index);

  if (slot === 0) {
    return {
      x: -book.width / 2,
      rotateY: 0,
      rotateZ: book.tilt * 0.35,
      origin: direction < 0 ? 'left' : 'right',
      markerX: book.width / 2,
    };
  }

  const sign = Math.sign(slot);
  // Reserve a stable central slot so narrower covers do not cancel the shelf's travel.
  let edge = (CENTER_SLOT_WIDTH / 2 + 24) * sign;

  for (let distance = 1; distance < Math.abs(slot); distance += 1) {
    edge +=
      sign *
      (getProjectedWidth(getBook(activeIndex + distance * sign), distance * sign) +
        (distance === 1 ? 16 : 17));
  }

  const angle = getAngle(slot);
  const coverProjection = book.width * Math.cos((Math.abs(angle) * Math.PI) / 180);
  const visibleWidth = getProjectedWidth(book, slot);
  const x = slot < 0 ? edge - coverProjection : edge - book.width + coverProjection;

  return {
    x,
    rotateY: angle,
    rotateZ: book.tilt,
    origin: slot < 0 ? 'left' : 'right',
    markerX: (slot < 0 ? edge - visibleWidth / 2 : edge + visibleWidth / 2) - x,
  };
};
