export interface PatchMembersMeAnonymityParams {
  displayAnonymous: boolean;
}

export type PatchMembersMeAnonymity = (params: PatchMembersMeAnonymityParams) => Promise<{
  accountStatus: string;
  nickname: string;
  profileImageUrl: string;
  displayAnonymous: boolean;
  anonymousNickname: string;
  memberId: number;
}>;
