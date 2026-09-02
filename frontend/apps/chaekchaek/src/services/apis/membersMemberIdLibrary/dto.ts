import type { ResponseDto } from '@/services/apis/api.types';

export interface GetMembersMemberIdLibraryRequestDto {
  pathParams: [{ name: 'memberId'; value: number }];
  query: {
    page: number;
    status: '' | 'WANT_TO_READ' | 'READING' | 'FINISHED';
    sort: string;
  };
}

export type GetMembersMemberIdLibraryResponseDto = ResponseDto<{
  filteredCount: number;
  nextPage: number;
  totalCount: number;
  items: {
    coverImageUrl: string;
    rating: number;
    title: string;
    commentCount: number;
    bookId: number;
    translators: string[];
    isbn13: string;
    totalPages: number;
    publisher: string;
    publishedDate: string;
    currentPage: number;
    category: string;
    status: string;
    authors: string[];
  }[];
}>;
