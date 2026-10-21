import { useRef } from 'react';

import html2canvas from 'html2canvas';

import { Button, ConstrainedImgBox, Dialog, Showcase } from '@chaekchaek/design-system';

import ConstrainedImgBoxDummyImg1 from '../../../../../../../packages/design-system/src/components/ConstrainedImgBox/imgs/dummy1.jpg';

import type { ShareLibraryImageDialogProps } from './ShareLibraryImageDialog.types';

const APP_NAME = '책췍';

export const ShareLibraryImageDialog = ({ onClose }: ShareLibraryImageDialogProps) => {
  const nickname = '먼지';
  const libraryName = '나를 넓히는 책들';

  const domRef = useRef(null);

  const download = async () => {
    if (!domRef.current) return;

    try {
      // 2. html2canvas로 지정한 ref 영역을 캔버스로 변환
      const canvas = await html2canvas(domRef.current, {
        scale: 2, // 기본값은 1, 숫자를 높이면 화질이 상승합니다 (선명도 확보)
        useCORS: true, // 외부 이미지가 포함되어 있을 경우 CORS 에러 방지
        logging: false, // 프로덕션 환경에서는 로그를 끕니다
      });

      // 2. 캔버스를 Data URL (base64) 형태로 변환
      const imageType = 'image/png';
      const imageData = canvas.toDataURL(imageType);

      // 3. 순수 JS로 임시 <a> 태그를 만들어 다운로드 트리거
      const link = document.createElement('a');
      link.href = imageData;
      link.download = `${APP_NAME}_${nickname}_${libraryName}.png`; // 저장될 파일명

      // 브라우저에 임시 링크를 추가했다가 클릭 후 제거
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (error) {
      console.error('화면 캡처 중 오류가 발생했습니다:', error);
    }
  };

  const handleClickDownload = async () => {
    await download();
    onClose();
  };

  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header>책장을 이미지로</Dialog.Header>
        <Dialog.Body>
          <Showcase ref={domRef}>
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
          <Button block variant="primary" onClick={handleClickDownload}>
            이미지 저장
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
