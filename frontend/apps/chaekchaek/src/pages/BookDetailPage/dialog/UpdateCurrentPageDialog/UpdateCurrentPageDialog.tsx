import { Dialog } from '@chaekchaek/design-system';
import { Field } from '@chaekchaek/design-system';
import { Input } from '@chaekchaek/design-system';
import { Button } from '@chaekchaek/design-system';

export const UpdateCurrentPageDialog = () => {
  return (
    <Dialog>
      <Dialog.Container>
        <Dialog.Header
          subTitle="이 감상은 160쪽 이후 내용을 포함해요. 내가 읽은 쪽수를 입력하면 읽은 범위까지 안
전하게 볼 수 있어요."
        >
          어디까지 읽으셨나요?
        </Dialog.Header>
        <Dialog.Body>
          <Field.Label>내가 읽은 쪽수</Field.Label>
          <Field.Content>
            <Input block />
          </Field.Content>
          {/* <Field.Description></Field.Description> */}
        </Dialog.Body>
        <Dialog.Footer>
          <Button variant="primary" block>
            입력한 쪽수까지 보기
          </Button>
          <Button variant="danger" block>
            스포일러 감수하고 전체 보기
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
