import type { ResponseDto } from '@/services/apis/api.types';

export interface GetMembersMemberIdLibraryRequestDto {
  pathParams: [{ name: 'memberId'; value: number }];
  query: {
    page: number;
    sort: string;
  };
}

export type GetMembersMemberIdLibraryResponseDto = ResponseDto<{
  member: {
    memberId: number;
    displayName: string;
    profileImageUrl: string;
  };
  filteredCount: number;
  nextPage: number;
  totalCount: number;
  items: {
    coverImageUrl: string;
    spineImageUrl: string | null;
    backImageUrl: string;
    rating: number;
    title: string;
    commentCount: number;
    bookId: number;
    translators: string[];
    isbn13: string;
    publisher: string;
    publishedDate: string;
    category: string;
    authors: string[];
  }[];
}>;
