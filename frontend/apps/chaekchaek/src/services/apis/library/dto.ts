import type { ResponseDto } from '@/services/apis/api.types';

export interface GetLibraryRequestDto {
  query: {
    page: number;
    status: '' | 'WANT_TO_READ' | 'READING' | 'FINISHED';
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
    authors: string[];
    translators: string[];
    publisher: string;
    category: string;
    publishedDate: string;
    totalPages: number;
    commentCount: number;
    status: string;
    currentPage: number;
    rating: number;
    addedAt: string;
    readingUpdatedAt: string;
  }[];
}>;
export interface PostLibraryRequestDto {
  data: { isbn13: string; totalPages?: number; status: string };
}

export type PostLibraryResponseDto = ResponseDto<undefined>;
