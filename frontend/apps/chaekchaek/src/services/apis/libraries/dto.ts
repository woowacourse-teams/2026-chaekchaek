import type { ResponseDto } from '@/services/apis/api.types';

export interface PostLibrariesRequestDto {
  data: { name: string };
}

export type PostLibrariesResponseDto = ResponseDto<undefined>;
export interface GetLibrariesRequestDto {
  query: { memberId: number };
}

export type GetLibrariesResponseDto = ResponseDto<{
  memberId: number;
  libraries: {
    libraryId: number;
    name: string;
    isDefault: boolean;
    totalCount: number;
    items: {
      bookId: number;
      title: string;
      spineImageUrl: string;
    }[];
  }[];
}>;
