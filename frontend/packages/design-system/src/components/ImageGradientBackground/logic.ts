export type RGB = {
  r: number;
  g: number;
  b: number;
};

const getRelativeLuminance = ({ r, g, b }: RGB): number => {
  const linearize = (channel: number) => {
    const value = channel / 255;

    return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  };

  return 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b);
};

export const getTextColor = (top: RGB, bottom: RGB): '#000000' | '#ffffff' => {
  const topLuminance = getRelativeLuminance(top);
  const bottomLuminance = getRelativeLuminance(bottom);
  const blackContrast = (Math.min(topLuminance, bottomLuminance) + 0.05) / 0.05;
  const whiteContrast = 1.05 / (Math.max(topLuminance, bottomLuminance) + 0.05);

  return blackContrast >= whiteContrast ? '#000000' : '#ffffff';
};

const getAverageColor = (data: Uint8ClampedArray): RGB => {
  let r = 0;
  let g = 0;
  let b = 0;
  let count = 0;

  for (let i = 0; i < data.length; i += 4) {
    const red = data[i];
    const green = data[i + 1];
    const blue = data[i + 2];
    const alphaValue = data[i + 3];

    if (
      red === undefined ||
      green === undefined ||
      blue === undefined ||
      alphaValue === undefined
    ) {
      continue;
    }

    const alpha = alphaValue / 255;

    if (alpha === 0) continue;

    r += red * alpha;
    g += green * alpha;
    b += blue * alpha;
    count += alpha;
  }

  if (count === 0) {
    return { r: 128, g: 128, b: 128 };
  }

  return {
    r: Math.round(r / count),
    g: Math.round(g / count),
    b: Math.round(b / count),
  };
};

export const extractBookColors = async (src: string) => {
  const image = new Image();
  image.crossOrigin = 'anonymous';
  image.src = src;

  await image.decode();

  const canvas = document.createElement('canvas');

  canvas.width = 100;
  canvas.height = 150;

  const context = canvas.getContext('2d');

  if (!context) {
    throw new Error('Canvas context를 생성할 수 없습니다.');
  }

  context.drawImage(image, 0, 0, 100, 150);

  // 상단 10% 영역
  const topData = context.getImageData(0, 0, 100, 15);

  // 하단 10% 영역
  const bottomData = context.getImageData(0, 135, 100, 15);

  return {
    top: getAverageColor(topData.data),
    bottom: getAverageColor(bottomData.data),
  };
};
