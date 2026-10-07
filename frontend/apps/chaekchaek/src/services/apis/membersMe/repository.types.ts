export interface GetMembersMeParams {}

export type GetMembersMe = (params: GetMembersMeParams) => Promise<{
  accountStatus: string;
  nickname: string;
  profileImageUrl: string;
  displayAnonymous: boolean;
  anonymousNickname: string;
  memberId: number;
  actorId: number;
  actorType: 'MEMBER' | 'GUEST';
}>;
export interface DeleteMembersMeParams {}

export type DeleteMembersMe = (params: DeleteMembersMeParams) => Promise<undefined>;
