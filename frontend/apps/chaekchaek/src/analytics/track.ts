import ReactGA from 'react-ga4';

export const track = (eventName: string, params?: Record<string, string | number | boolean>) => {
  ReactGA.event(eventName, params);
};
