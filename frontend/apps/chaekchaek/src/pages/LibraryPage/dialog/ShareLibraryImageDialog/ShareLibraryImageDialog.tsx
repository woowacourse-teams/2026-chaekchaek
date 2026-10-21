import {
  Button,
  ButtonStack,
  ConstrainedImgBox,
  Dialog,
  Showcase,
} from '@chaekchaek/design-system';

import ConstrainedImgBoxDummyImg1 from '../../../../../../../packages/design-system/src/components/ConstrainedImgBox/imgs/dummy1.jpg';

import type { ShareLibraryImageDialogProps } from './ShareLibraryImageDialog.types';

export const ShareLibraryImageDialog = ({ onClose }: ShareLibraryImageDialogProps) => {
  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header>책장을 이미지로</Dialog.Header>
        <Dialog.Body>
          <Showcase>
            {Array.from({ length: 10 }).map((_, index) => {
              return (
                <ConstrainedImgBox
                  img={ConstrainedImgBoxDummyImg1}
                  maxWidth="80px"
                  maxHeight={'170px'}
                />
              );
            })}
          </Showcase>
        </Dialog.Body>
        <Dialog.Footer>
          <ButtonStack>
            <Button block variant="ghost">
              책장 선택
            </Button>
            <Button block1 variant="primary">
              이미지 저장
            </Button>
          </ButtonStack>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
