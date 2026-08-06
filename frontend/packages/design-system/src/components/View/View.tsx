import type { Props } from './';

const classnameDefault = 'ui-View';

export const View = (props: Props) => {
  const { ...restProps } = props;

  const classname = classnameDefault;

  return <div className={classname} {...restProps} />;
};
