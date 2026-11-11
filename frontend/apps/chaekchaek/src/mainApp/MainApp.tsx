import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';
import { UpdateNicknameDialog } from '@/pages/LegacyLibraryPage/dialog/UpdateNicknameDialog';
import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';

type MainAppDialogType = 'UpdateNicknameDialog';

export const MainApp = () => {
  const { user } = useAuthContext();

  useEffect(() => {
    const isVisibleUpdateNickname = user && !user.nickname;

    if (isVisibleUpdateNickname) {
      handleOpenDialog('UpdateNicknameDialog');
    }
  }, [user]);

  const [dialog, setDialog] = useState<MainAppDialogType | null>(null);
  const handleOpenDialog = (dialog: MainAppDialogType) => {
    setDialog(dialog);
  };
  const handleCloseDialog = () => {
    setDialog(null);
  };

  const renderDialog = (dialog: MainAppDialogType | null) => {
    switch (dialog) {
      case 'UpdateNicknameDialog':
        return <UpdateNicknameDialog onClose={handleCloseDialog} />;

      default:
        return null;
    }
  };

  const dialogElement = renderDialog(dialog);

  return (
    <>
      {dialogElement}
      <Outlet />
    </>
  );
};
