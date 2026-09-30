const GOOGLE_PLAY_URL =
  'https://play.google.com/store/apps/details?id=com.chamsae.chaekchaek&utm_source=qr&utm_medium=offline&utm_campaign=offline_qr_2026_10';

const APP_STORE_URL =
  'https://apps.apple.com/kr/app/id6804956142?pt=129343028&ct=offline_qr_2026_10%20QR&mt=8';

export const resolveStoreDestination = (userAgent: string) => {
  if (/Android/i.test(userAgent)) return GOOGLE_PLAY_URL;
  if (/iPhone|iPod/i.test(userAgent)) return APP_STORE_URL;
  return null;
};
