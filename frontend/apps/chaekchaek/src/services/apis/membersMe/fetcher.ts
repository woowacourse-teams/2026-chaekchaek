import { instance } from '@/services/core/http';

import type { GetMembersMeRequestDto, GetMembersMeResponseDto } from './dto';

export const getMembersMe =
  async ({}: GetMembersMeRequestDto): Promise<GetMembersMeResponseDto> => {
    const response = await instance(
      '/api/v1/members/me',
      {
        method: 'get',
      },
      {
        redirectOnReissueFailure: false,
      },
    );

    return response.data;
  };

import type { DeleteMembersMeRequestDto, DeleteMembersMeResponseDto } from './dto';

export const deleteMembersMe =
  async ({}: DeleteMembersMeRequestDto): Promise<DeleteMembersMeResponseDto> => {
    const response = await instance('/api/v1/members/me', {
      method: 'delete',
    });

    return response.data;
  };
