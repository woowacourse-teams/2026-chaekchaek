import type { ReactNode } from 'react';

export type UserData = {
  accountStatus: string;
  nickname: string;
  profileImageUrl: string;
  displayAnonymous: boolean;
  anonymousNickname: string;
  memberId: number;
  actorId: number;
  actorType: 'MEMBER' | 'GUEST';
};

export type GuestData = {
  guestToken: string;
  nickname: string;
  expiresAt: string;
  actorId: number;
  actorType: 'MEMBER' | 'GUEST';
};

export type Props = {
  children: ReactNode;
};
