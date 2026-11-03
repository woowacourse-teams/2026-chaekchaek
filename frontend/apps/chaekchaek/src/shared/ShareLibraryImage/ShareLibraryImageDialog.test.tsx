import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import html2canvas from 'html2canvas';
import { describe, expect, it, vi } from 'vitest';

import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';

import { ShareLibraryImageDialog } from './ShareLibraryImageDialog';
import { libraryImageDataUrl, libraryToShare } from './ShareLibraryImageDialog.fixtures';

vi.mock('html2canvas', () => ({ default: vi.fn() }));

describe('ShareLibraryImageDialog', () => {
  it('이미지 저장 버튼을 클릭하면 책장 이미지가 다운로드 된다', async () => {
    const user = userEvent.setup();
    const canvas = document.createElement('canvas');
    const toDataURL = vi.spyOn(canvas, 'toDataURL').mockReturnValue(libraryImageDataUrl);
    vi.mocked(html2canvas).mockResolvedValue(canvas);
    const downloadImage = vi.fn<(href: string, filename: string, connected: boolean) => void>();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      downloadImage(this.href, this.download, this.isConnected);
    });

    // 책장 이미지 공유 팝업을 연다.
    renderProvider(<ShareLibraryImageDialog {...libraryToShare} onClose={vi.fn()} />, {
      auth: defaultLoggedAuthContextValue,
    });

    // 저장할 책장 이름과 사용자 이름이 표시되는지 확인한다.
    expect(screen.getByText(libraryToShare.libraryName)).toBeVisible();
    expect(screen.getByText(`${defaultLoggedAuthContextValue.user.nickname}의 책장`)).toBeVisible();

    // 이미지 저장 버튼을 클릭한다.
    await user.click(screen.getByRole('button', { name: '이미지 저장', exact: true }));

    // 표시된 책장이 PNG 이미지로 변환되어 사용자 이름과 책장 이름이 담긴 파일로 다운로드되는지 확인한다.
    await waitFor(() => {
      expect(downloadImage).toHaveBeenCalledExactlyOnceWith(
        libraryImageDataUrl,
        `책췍_${defaultLoggedAuthContextValue.user.nickname}_${libraryToShare.libraryName}.png`,
        true,
      );
    });
    expect(html2canvas).toHaveBeenCalledTimes(1);
    const capturedLibrary = vi.mocked(html2canvas).mock.calls[0][0];
    expect(capturedLibrary).toContainElement(screen.getByText(libraryToShare.libraryName));
    expect(capturedLibrary).toHaveTextContent(`${defaultLoggedAuthContextValue.user.nickname}의 책장`);
    expect(capturedLibrary.querySelector('img')).toHaveAttribute('src', libraryToShare.imgs[0].url);
    expect(toDataURL).toHaveBeenCalledExactlyOnceWith('image/png');
  });
});
