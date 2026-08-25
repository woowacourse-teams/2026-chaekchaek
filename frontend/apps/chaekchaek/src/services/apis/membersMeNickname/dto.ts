import type { ResponseDto } from '@/services/apis/api.types';

export interface PatchMembersMeNicknameRequestDto {
  data: { nickname: string };
}

export type PatchMembersMeNicknameResponseDto = ResponseDto<{
  accountStatus: string;
  nickname: string;
  profileImageUrl: string;
  displayAnonymous: boolean;
  anonymousNickname: string;
  memberId: number;
}>;
