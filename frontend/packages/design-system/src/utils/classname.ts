import clsx from 'clsx';

type Modifiers = Record<string, string | false | undefined>;

type Options = {
  styles: Record<string, string>;
  baseName: string;
  modifiers?: Modifiers;
  className?: string | undefined;
};

export const createClassName = ({ styles, baseName, modifiers, className }: Options) => {
  return clsx(
    baseName,
    styles?.[baseName],
    styles?.root,
    modifiers && Object.values(modifiers),
    className,
  );
};
