import { Button, ButtonStack, Dialog, Radio, Checkbox, Title } from '@chaekchaek/design-system';

import type { Props } from './PMFDialog.types';

export const PMFDialog = (props: Props) => {
  const { onClose } = props;
  return (
    <Dialog size="large" onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header>책췍 사용자 설문(1/4)</Dialog.Header>
        <Dialog.Body>
          <Title sx={{ mb: 4 }} level="main" subtitle="하나만 선택해 주세요">
            이 제품을 더 이상 쓸 수 없다면 어떻겠습니까?
          </Title>

          {[
            {
              answerCode: 'VERY_DISAPPOINTED',
              label: '매우 실망',
            },
            {
              answerCode: 'SLIGHTLY_DISAPPOINTED',
              label: '약간 실망',
            },
            {
              answerCode: 'NOT_DISAPPOINTED',
              label: '실망하지 않음',
            },
            {
              answerCode: 'NO_LONGER_USING',
              label: '더 이상 쓰지 않음',
            },
          ].map((option) => {
            return (
              <Radio sx={{ mt: 2 }} block size="large" id="PMF_CORE" value={option.answerCode}>
                {option.label}
              </Radio>
            );
          })}
        </Dialog.Body>
        <Dialog.Footer>
          <ButtonStack>
            {/* <Button variant="ghost">이전</Button> */}
            <Button variant="primary">다음</Button>
          </ButtonStack>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
