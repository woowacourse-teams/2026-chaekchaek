import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { it, expect, vi } from 'vitest';

import { ENV } from '@/configs/env';
import { server } from '@/mocks/msw/server';
import { defaultLoggedAuthContextValue, renderProvider } from '@/test/utils/render';
import {
  bookToRegister,
  bookToRemove,
  registeredBookResponse,
  librariesBeforeBookRegistrationResponse,
  librariesBeforeBookRemovalResponse,
  selectedLibraryResponse,
  unselectedLibraryResponse,
  unchangedBookSelectionLibrariesResponse,
  unselectedLibraryForUnchangedSelectionResponse,
} from '../../BookDetailPage.fixtures';
import { RegisterBookToLibraryDialog } from './RegisterBookToLibraryDialog';

it('선택하지 않았던 책장을 선택하고 완료하면 해당 책장에 책이 등록된다', async () => {
  const user = userEvent.setup();
  const onClose = vi.fn();
  const addBook = vi.fn();
  const removeBook = vi.fn();
  server.use(
    http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () =>
      HttpResponse.json(structuredClone(librariesBeforeBookRegistrationResponse)),
    ),
    http.post(`${ENV.APP_API_URL}/api/v1/libraries/:libraryId`, async ({ params, request }) => {
      addBook(Number(params.libraryId), await request.json());
      return HttpResponse.json(structuredClone(registeredBookResponse), { status: 201 });
    }),
    http.delete(`${ENV.APP_API_URL}/api/v1/libraries/:libraryId/books/:bookId`, ({ params }) => {
      removeBook(Number(params.libraryId), Number(params.bookId));
      return new HttpResponse(null, { status: 204 });
    }),
  );
  renderProvider(
    <RegisterBookToLibraryDialog
      book={{ isbn: bookToRegister.isbn13, bookId: bookToRegister.bookId }}
      onClose={onClose}
    />,
    { auth: defaultLoggedAuthContextValue },
  );

  // 책장 선택 팝업에서 아직 선택하지 않은 책장을 확인한다.
  const libraryName = await screen.findByText(unselectedLibraryResponse.name);
  const libraryItem = libraryName.closest('li');
  if (!libraryItem) throw new Error('선택할 책장의 목록 항목을 찾을 수 없습니다.');
  const checkbox = within(libraryItem).getByRole('checkbox');
  expect(checkbox).not.toBeChecked();

  // 책장을 선택하고 선택 상태가 표시되는지 확인한다.
  await user.click(checkbox);
  expect(checkbox).toBeChecked();

  expect(onClose).not.toHaveBeenCalled();
  expect(addBook).not.toHaveBeenCalled();
  expect(removeBook).not.toHaveBeenCalled();
  await user.click(screen.getByRole('button', { name: '이 책장에 담기' }));
  await waitFor(() => expect(onClose).toHaveBeenCalledExactlyOnceWith());
  expect(addBook).toHaveBeenCalledExactlyOnceWith(unselectedLibraryResponse.libraryId, {
    isbn13: bookToRegister.isbn13,
    status: 'READING',
  });
  expect(removeBook).not.toHaveBeenCalled();
});

it('선택되어 있던 책장을 해제하고 완료하면 해당 책장에서 책이 제거된다', async () => {
  const user = userEvent.setup();
  const onClose = vi.fn();
  const addBook = vi.fn();
  const removeBook = vi.fn();
  server.use(
    http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () =>
      HttpResponse.json(structuredClone(librariesBeforeBookRemovalResponse)),
    ),
    http.post(`${ENV.APP_API_URL}/api/v1/libraries/:libraryId`, async ({ params, request }) => {
      addBook(Number(params.libraryId), await request.json());
      return HttpResponse.json(structuredClone(registeredBookResponse), { status: 201 });
    }),
    http.delete(`${ENV.APP_API_URL}/api/v1/libraries/:libraryId/books/:bookId`, ({ params }) => {
      removeBook(Number(params.libraryId), Number(params.bookId));
      return new HttpResponse(null, { status: 204 });
    }),
  );
  renderProvider(
    <RegisterBookToLibraryDialog
      book={{ isbn: bookToRemove.isbn13, bookId: bookToRemove.bookId }}
      onClose={onClose}
    />,
    { auth: defaultLoggedAuthContextValue },
  );

  // 책이 등록된 책장이 선택되어 있는지 확인한다.
  const libraryName = await screen.findByText(selectedLibraryResponse.name);
  const libraryItem = libraryName.closest('li');
  if (!libraryItem) throw new Error('선택을 해제할 책장의 목록 항목을 찾을 수 없습니다.');
  const checkbox = within(libraryItem).getByRole('checkbox');
  expect(checkbox).toBeChecked();

  // 선택되어 있던 책장을 눌러 선택을 해제한다.
  await user.click(checkbox);
  expect(checkbox).not.toBeChecked();

  expect(onClose).not.toHaveBeenCalled();
  expect(addBook).not.toHaveBeenCalled();
  expect(removeBook).not.toHaveBeenCalled();
  await user.click(screen.getByRole('button', { name: '이 책장에 담기' }));
  await waitFor(() => expect(onClose).toHaveBeenCalledExactlyOnceWith());
  expect(removeBook).toHaveBeenCalledExactlyOnceWith(
    selectedLibraryResponse.libraryId,
    bookToRemove.bookId,
  );
  expect(addBook).not.toHaveBeenCalled();
});

it('책장 선택을 변경하지 않고 완료하면 기존 상태가 유지된다', async () => {
  const user = userEvent.setup();
  const onClose = vi.fn();
  const addBook = vi.fn();
  const removeBook = vi.fn();
  server.use(
    http.get(`${ENV.APP_API_URL}/api/v1/libraries`, () =>
      HttpResponse.json(structuredClone(unchangedBookSelectionLibrariesResponse)),
    ),
    http.post(`${ENV.APP_API_URL}/api/v1/libraries/:libraryId`, async ({ params, request }) => {
      addBook(Number(params.libraryId), await request.json());
      return HttpResponse.json(structuredClone(registeredBookResponse), { status: 201 });
    }),
    http.delete(`${ENV.APP_API_URL}/api/v1/libraries/:libraryId/books/:bookId`, ({ params }) => {
      removeBook(Number(params.libraryId), Number(params.bookId));
      return new HttpResponse(null, { status: 204 });
    }),
  );
  renderProvider(
    <RegisterBookToLibraryDialog
      book={{ isbn: registeredBookResponse.isbn13, bookId: registeredBookResponse.bookId }}
      onClose={onClose}
    />,
    { auth: defaultLoggedAuthContextValue },
  );

  // 책이 등록된 책장은 선택되어 있고, 등록되지 않은 책장은 선택되어 있지 않은지 확인한다.
  const selectedLibraryName = await screen.findByText(selectedLibraryResponse.name);
  const selectedLibraryItem = selectedLibraryName.closest('li');
  if (!selectedLibraryItem) throw new Error('선택된 책장의 목록 항목을 찾을 수 없습니다.');
  expect(within(selectedLibraryItem).getByRole('checkbox')).toBeChecked();
  const unselectedLibraryName = await screen.findByText(
    unselectedLibraryForUnchangedSelectionResponse.name,
  );
  const unselectedLibraryItem = unselectedLibraryName.closest('li');
  if (!unselectedLibraryItem) throw new Error('선택되지 않은 책장의 목록 항목을 찾을 수 없습니다.');
  expect(within(unselectedLibraryItem).getByRole('checkbox')).not.toBeChecked();

  expect(onClose).not.toHaveBeenCalled();
  expect(addBook).not.toHaveBeenCalled();
  expect(removeBook).not.toHaveBeenCalled();
  await user.click(screen.getByRole('button', { name: '이 책장에 담기' }));
  await waitFor(() => expect(onClose).toHaveBeenCalledExactlyOnceWith());
  expect(addBook).not.toHaveBeenCalled();
  expect(removeBook).not.toHaveBeenCalled();
  expect(within(selectedLibraryItem).getByRole('checkbox')).toBeChecked();
  expect(within(unselectedLibraryItem).getByRole('checkbox')).not.toBeChecked();
});
