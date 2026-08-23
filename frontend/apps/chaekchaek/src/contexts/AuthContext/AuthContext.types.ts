import type { ReactNode } from 'react';

export type UserData = {
  accountStatus: string;
  nickname: string;
  profileImageUrl: string;
  displayAnonymous: boolean;
  anonymousNickname: string;
  memberId: number;
};

export type Props = {
  children: ReactNode;
};
