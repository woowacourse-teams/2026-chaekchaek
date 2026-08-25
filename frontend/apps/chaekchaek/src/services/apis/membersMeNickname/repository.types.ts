export interface PatchMembersMeNicknameParams {
  nickname: string;
}

export type PatchMembersMeNickname = (params: PatchMembersMeNicknameParams) => Promise<{
  accountStatus: string;
  nickname: string;
  profileImageUrl: string;
  displayAnonymous: boolean;
  anonymousNickname: string;
  memberId: number;
}>;
