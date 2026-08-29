import { useCallback, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

import {
  Button,
  Checkbox,
  Icon,
  IconButton,
  ImgBox,
  List,
  Notice,
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
import { patchMembersMeAnonymity } from '@/services/apis/membersMeAnonymity/repository';
import { useExecute } from '@/services/core/useExecute';
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

  const filteredTotalPages = libraryData ? Math.ceil(libraryData.filteredCount / 10) : 1;

  const [isEditing, setIsEditing] = useState(false);
  const handleClickToggleEdit = () => {
    setIsEditing((prev) => !prev);
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

  const { user, login } = useAuthContext();
  const { status: anonymityStatus, mutate: updateAnonymity } = useExecute({
    executeFn: patchMembersMeAnonymity,
  });

  const handleToggleAnonymous = async () => {
    if (!user) return;

    if (user.displayAnonymous) {
      if (!user.nickname) return handleOpenDialog('UpdateNicknameDialog');

      const userWithAnonymityDisabled = await updateAnonymity({ displayAnonymous: false });
      if (!userWithAnonymityDisabled) return;

      login(userWithAnonymityDisabled);

      return;
    }

    const updatedUser = await updateAnonymity({ displayAnonymous: true });
    if (!updatedUser) return;

    login(updatedUser);
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
        <Split>
          <Split.Top sx={{ mb: 6 }}>
            <Title
              level="page"
              trailing={
                <>
                  {(user !== null || true) && (
                    <>
                      {user?.displayAnonymous && (
                        <Button
                          leading={<Icon.CheckboxOffIcon />}
                          variant="soft"
                          disabled={anonymityStatus.status === 'loading'}
                          onClick={handleToggleAnonymous}
                        >
                          익명 감상 비공개
                        </Button>
                      )}
                      {!user?.displayAnonymous && (
                        <Button
                          leading={<Icon.CheckboxOnIcon />}
                          variant="soft"
                          disabled={anonymityStatus.status === 'loading'}
                          onClick={handleToggleAnonymous}
                        >
                          익명 감상 비공개
                        </Button>
                      )}
                    </>
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
                        leading={<Icon.TrashIcon color="error" />}
                        variant="danger-weak"
                        disabled={!isAbleDeleteStatus}
                        onClick={() => handleOpenDialog('DeleteBooksDialog')}
                      >
                        {!!bookSelection.length && `${bookSelection.length}권`} 삭제
                      </Button>
                    </>
                  )}
                  <Button variant="primary" onClick={handleClickToggleEdit}>
                    {!isEditing ? '서재 편집' : '편집 종료'}
                  </Button>
                </>
              }
            >
              내 서재
            </Title>
          </Split.Top>
          <Split.Side>
            <Title
              level="main"
              orientation="vertical"
              trailing={
                <OptionList
                  sx={{ mt: 3 }}
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
            {!libraryData?.items.length && (
              <Notice height={500}>내 서재에 등록된 책이 없습니다.</Notice>
            )}
            <List columns={2}>
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
                      <Link to={`/books/${item.isbn13}`}>
                        <ImgBox img={item.coverImageUrl} size="small" />
                      </Link>
                    </List.Item.Leading>
                    <List.Item.Content
                      as={Link}
                      to={`/books/${item.isbn13}`}
                      title={
                        <>
                          <Tag
                            sx={{ mb: 2 }}
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
                      {!isEditing && <Icon.ArrowRightIcon />}
                      {isEditing && (
                        <>
                          <IconButton
                            sx={{ ml: 4 }}
                            onClick={() => {
                              handleClickDelete(item.bookId);
                            }}
                          >
                            <Icon.TrashIcon color="error" />
                          </IconButton>
                        </>
                      )}
                    </List.Item.Trailing>
                  </List.Item>
                );
              })}
            </List>

            <Pagination
              sx={{ mt: 5 }}
              defaultPage={defaultPage}
              totalPages={filteredTotalPages}
              onChange={handleChangeDefaultPage}
            />
          </Split.Content>
        </Split>
        {dialogElement}
      </Main>
    </Layout>
  );
};
