import type { ResponseDto } from '@/services/apis/api.types';

export interface GetMembersMeRequestDto {}

export type GetMembersMeResponseDto = ResponseDto<{
  accountStatus: string;
  nickname: string;
  profileImageUrl: string;
  displayAnonymous: boolean;
  anonymousNickname: string;
  memberId: number;
}>;
export interface DeleteMembersMeRequestDto {}

export type DeleteMembersMeResponseDto = ResponseDto<undefined>;
