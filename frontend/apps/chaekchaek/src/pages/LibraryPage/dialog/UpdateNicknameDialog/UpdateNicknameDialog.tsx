import type { FormEvent } from 'react';

import { Button, ButtonStack, Dialog, Field, Input } from '@chaekchaek/design-system';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { useFormValues } from '@/hooks/useFormValues';

import { patchMembersMeNickname } from '@/services/apis/membersMeNickname/repository';
import { useExecute } from '@/services/core/useExecute';

import { validateNickname } from './validator';
import type { NicknameFormValues } from './validator';

import type { UpdateNicknameDialogProps } from './UpdateNicknameDialog.types';

export const UpdateNicknameDialog = ({ onClose }: UpdateNicknameDialogProps) => {
  const { user, login } = useAuthContext();

  const { values, errors, onChange, isValid, valids } = useFormValues<NicknameFormValues>({
    initialValues: {
      nickname: user?.nickname ?? '',
    },
    validate: validateNickname,
  });

  const { mutate } = useExecute({ executeFn: patchMembersMeNickname });

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const updatedUser = await mutate({ nickname: values.nickname });
    if (!updatedUser) return;

    login(updatedUser);
    onClose();
  };

  return (
    <Dialog size="medium" onClose={onClose}>
      <Dialog.Container>
        <form onSubmit={handleSubmit}>
          <Dialog.Header subTitle="지금부터 감상과 답글에 이 닉네임이 표시됩니다.">
            닉네임 설정
          </Dialog.Header>

          <Dialog.Body>
            <Field>
              <Field.Label as="label" htmlFor="nickname">
                닉네임
              </Field.Label>
              <Field.Content>
                <Input
                  id="nickname"
                  name="nickname"
                  placeholder="닉네임을 입력하세요"
                  value={values.nickname}
                  onChange={onChange}
                />
              </Field.Content>
              <Field.Description>
                {valids.nickname
                  ? '2-15자 · 이미 사용 중인 닉네임은 쓸 수 없어요'
                  : errors.nickname.find(({ valid }) => !valid)?.message}
              </Field.Description>
            </Field>
          </Dialog.Body>

          <Dialog.Footer>
            <ButtonStack>
              <Button type="button" variant="ghost" size="large" block onClick={onClose}>
                취소
              </Button>
              <Button type="submit" variant="primary" size="large" block disabled={!isValid}>
                확인
              </Button>
            </ButtonStack>
          </Dialog.Footer>
        </form>
      </Dialog.Container>
    </Dialog>
  );
};
