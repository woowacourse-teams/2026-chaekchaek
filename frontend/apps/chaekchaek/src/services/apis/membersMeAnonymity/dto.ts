import type { ResponseDto } from '@/services/apis/api.types';

export interface PatchMembersMeAnonymityRequestDto {
  data: { displayAnonymous: boolean };
}

export type PatchMembersMeAnonymityResponseDto = ResponseDto<{
  accountStatus: string;
  nickname: string;
  profileImageUrl: string;
  displayAnonymous: boolean;
  anonymousNickname: string;
  memberId: number;
}>;
