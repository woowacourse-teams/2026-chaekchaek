export const space = {
  0: '0',
  1: '4px',
  2: '8px',
  3: '12px',
  4: '16px',
  5: '20px',
  6: '24px',
  8: '32px',
  10: '40px',
} as const;

type SpaceType = keyof typeof space;

export type sx = {
  m?: SpaceType;
  mt?: SpaceType;
  mr?: SpaceType;
  mb?: SpaceType;
  ml?: SpaceType;
};

export type SpacingType = {
  sx?: sx | undefined;
};

export const resolveSx = ({ sx }: { sx?: sx | undefined }) => {
  if (!sx) return;
  return {
    ...(sx.m && { margin: space[sx.m] }),
    ...(sx.mt && { marginTop: space[sx.mt] }),
    ...(sx.mr && { marginRight: space[sx.mr] }),
    ...(sx.mb && { marginBottom: space[sx.mb] }),
    ...(sx.ml && { marginLeft: space[sx.ml] }),
  };
};
