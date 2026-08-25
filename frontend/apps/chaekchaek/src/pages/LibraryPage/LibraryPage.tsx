import { useCallback, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import {
  Button,
  Checkbox,
  ImgBox,
  List,
  OptionList,
  Pagination,
  ProgressBar,
  Select,
  Split,
  Tag,
  Title,
} from '@chaekchaek/design-system';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';

import { getLibrary } from '@/services/apis/library/repository';
import { useLoadData } from '@/services/core/useLoadData';

import { UpdateBookStatusDialog } from './dialog/UpdateBookStatusDialog';
import { DeleteBooksDialog } from './dialog/DeleteBooksDialog';
import { UpdateNicknameDialog } from './dialog/UpdateNicknameDialog';
import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

export const READING_STATUS = {
  ALL: 'ALL',
  WANT_TO_READ: 'WANT_TO_READ',
  READING: 'READING',
  FINISHED: 'FINISHED',
} as const;

export type ReadingStatus = (typeof READING_STATUS)[keyof typeof READING_STATUS];

export const READING_STATUS_LABELS: Record<ReadingStatus, string> = {
  [READING_STATUS.ALL]: '전체',
  [READING_STATUS.WANT_TO_READ]: '읽고 싶어요',
  [READING_STATUS.READING]: '읽는 중',
  [READING_STATUS.FINISHED]: '다 읽음',
};

const READING_SORT = {
  RECENT: 'RECENT',
  OLDEST: 'OLDEST',
  COMMENT: 'COMMENT',
  RATING: 'RATING',
  TITLE: 'TITLE',
} as const;

type ReadingSort = (typeof READING_SORT)[keyof typeof READING_SORT];

const READING_SORT_LABELS = {
  RECENT: '최근순',
  OLDEST: '오래된순',
  COMMENT: '감상순',
  RATING: '별점순',
  TITLE: '제목순',
} as const;

export const LibraryPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const defaultPage = searchParams.get('page') ? Number(searchParams.get('page')) : 1;
  const status = (searchParams.get('status') ?? READING_STATUS.ALL) as ReadingStatus;
  const sort = (searchParams.get('sort') ?? READING_SORT.RECENT) as ReadingSort;

  const handleChangeDefaultPage = (defaultPage: number) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('page', String(defaultPage));
      return next;
    });
  };

  const handleChangeStatus = (status: ReadingStatus) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('status', status);
      next.set('page', '1');
      return next;
    });
  };

  const handleChangeSort = (sort: ReadingSort) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('sort', sort);
      next.set('page', '1');
      return next;
    });
  };

  const getLibraryLoadData = useCallback(async () => {
    return await getLibrary({
      page: defaultPage,
      sort,
      status,
    });
  }, [defaultPage, status, sort]);
  const {
    refetch: refetchGetLibrary,
    status: { data: libraryData },
  } = useLoadData({ queryFn: getLibraryLoadData });

  const [isEditing, setIsEditing] = useState(false);
  const handleClickStartEdit = () => {
    setIsEditing(true);
  };
  const handleClickEndEdit = () => {
    setIsEditing(false);
  };

  const [bookSelection, setBookSelection] = useState<number[]>([]);
  const handleChangeBookSelection = (bookId: number) => {
    setBookSelection((prev) =>
      prev.includes(bookId) ? prev.filter((v) => v !== bookId) : [...prev, bookId],
    );
  };
  const handleResetBookSelection = () => {
    setBookSelection([]);
  };

  const handleBookStatusUpdated = () => {
    refetchGetLibrary();

    handleCloseDialog();
    handleClickEndEdit();
    handleResetBookSelection();
    handleCloseDialog();
  };

  const handleBooksDeleted = () => {
    refetchGetLibrary();

    handleCloseDialog();
    handleClickEndEdit();
    handleResetBookSelection();
  };

  const handleClickDelete = (bookId: number) => {
    handleChangeBookSelection(bookId);
    handleOpenDialog('DeleteBooksDialog');
  };

  const isAbleUpdateStatus = bookSelection.length;
  const isAbleDeleteStatus = bookSelection.length;

  const { user } = useAuthContext();

  const handleToggleAnonymous = () => {
    if (user?.displayAnonymous) {
      return;
    }

    handleOpenDialog('UpdateNicknameDialog');
  };

  const [dialog, setDialog] = useState<
    'UpdateBookStatusDialog' | 'DeleteBooksDialog' | 'UpdateNicknameDialog' | null
  >(null);
  const handleOpenDialog = (
    dialog: 'UpdateBookStatusDialog' | 'DeleteBooksDialog' | 'UpdateNicknameDialog',
  ) => {
    setDialog(dialog);
  };
  const handleCloseDialog = () => {
    setDialog(null);
  };

  const renderDialog = (
    dialog: 'UpdateBookStatusDialog' | 'DeleteBooksDialog' | 'UpdateNicknameDialog' | null,
  ) => {
    switch (dialog) {
      case 'UpdateBookStatusDialog':
        return (
          <UpdateBookStatusDialog
            bookSelection={bookSelection}
            onBookStatusUpdated={handleBookStatusUpdated}
            onClose={handleCloseDialog}
          />
        );
      case 'DeleteBooksDialog':
        return (
          <DeleteBooksDialog
            bookIds={bookSelection}
            onBooksDeleted={handleBooksDeleted}
            onClose={handleCloseDialog}
          />
        );
      case 'UpdateNicknameDialog':
        return <UpdateNicknameDialog onClose={handleCloseDialog} />;

      default:
        return null;
    }
  };

  const dialogElement = renderDialog(dialog);

  return (
    <Layout>
      <Header />
      <Main>
        <Title
          level="page"
          trailing={
            <>
              {user !== null && (
                <Button variant="ghost" onClick={() => handleToggleAnonymous()}>
                  {user?.displayAnonymous ? '익명 감상 비공개' : '감상 익명 공개'}
                </Button>
              )}
              {isEditing && (
                <>
                  <Button
                    variant="ghost"
                    disabled={!isAbleUpdateStatus}
                    onClick={() => handleOpenDialog('UpdateBookStatusDialog')}
                  >
                    상태 변경
                  </Button>
                  <Button
                    variant="ghost"
                    disabled={!isAbleDeleteStatus}
                    onClick={() => handleOpenDialog('DeleteBooksDialog')}
                  >
                    삭제
                  </Button>
                </>
              )}
              <Button variant="primary" onClick={handleClickStartEdit}>
                서재 편집
              </Button>
            </>
          }
        >
          내 서재
        </Title>
        <Split>
          <Split.Side>
            <Title
              level="main"
              orientation="vertical"
              trailing={
                <OptionList
                  value={status}
                  options={Object.entries(READING_STATUS_LABELS).map(([labelKey, labelValue]) => {
                    return {
                      value: labelKey,
                      text: labelValue,
                    };
                  })}
                  onChange={handleChangeStatus}
                />
              }
            >
              독서 상태
            </Title>
          </Split.Side>
          <Split.Content>
            <Title
              level="caption"
              trailing={
                <>
                  <Select
                    value={sort}
                    options={Object.entries(READING_SORT_LABELS).map(([labelKey, labelValue]) => {
                      return {
                        value: labelKey,
                        text: labelValue,
                      };
                    })}
                    onChange={handleChangeSort}
                  />
                </>
              }
            >
              독서 상태
            </Title>
            <List>
              {libraryData?.items.map((item) => {
                const isIncluded = bookSelection.includes(item.bookId);

                return (
                  <List.Item key={item.bookId}>
                    <List.Item.Leading>
                      {isEditing && (
                        <Checkbox
                          checked={isIncluded}
                          onChange={() => {
                            handleChangeBookSelection(item.bookId);
                          }}
                        />
                      )}
                      <a href={`/books/${item.isbn13}`}>
                        <ImgBox img={item.coverImageUrl} size="small" />
                      </a>
                    </List.Item.Leading>
                    <List.Item.Content
                      title={
                        <>
                          <Tag
                            variant={item.status === READING_STATUS.READING ? 'primary' : 'subtle'}
                          >
                            {READING_STATUS_LABELS?.[item.status as ReadingStatus] ?? ''}
                          </Tag>
                          <br />
                          {item.title}
                        </>
                      }
                      content={item.authors.join(' · ')}
                      description={
                        <>
                          {item.totalPages > 0 && (
                            <ProgressBar value={item.currentPage} max={item.totalPages} />
                          )}
                        </>
                      }
                    />
                    <List.Item.Trailing>
                      {!isEditing && <>&gt;</>}
                      {isEditing && (
                        <Button
                          onClick={() => {
                            handleClickDelete(item.bookId);
                          }}
                        >
                          삭제
                        </Button>
                      )}
                    </List.Item.Trailing>
                  </List.Item>
                );
              })}
            </List>

            <Pagination
              defaultPage={1}
              totalPages={libraryData?.filteredCount ?? 1}
              onChange={handleChangeDefaultPage}
            />
          </Split.Content>
        </Split>
        {dialogElement}
      </Main>
    </Layout>
  );
};
