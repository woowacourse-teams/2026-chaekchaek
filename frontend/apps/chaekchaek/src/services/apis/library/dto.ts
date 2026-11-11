import type { ResponseDto } from '@/services/apis/api.types';

export interface GetLibraryRequestDto {
  query: {
    page: number;
    sort: string;
  };
}

export type GetLibraryResponseDto = ResponseDto<{
  totalCount: number;
  filteredCount: number;
  nextPage: number;
  items: {
    bookId: number;
    isbn13: string;
    title: string;
    coverImageUrl: string;
    spineImageUrl: string;
    backImageUrl: string;
    authors: string[];
    translators: string[];
    publisher: string;
    category: string;
    publishedDate: string;
    commentCount: number;
    rating: number;
    addedAt: string;
  }[];
}>;
export interface PostLibraryRequestDto {
  data: { isbn13: string };
}

export type PostLibraryResponseDto = ResponseDto<undefined>;
