import type { ResponseDto } from '@/services/apis/api.types';

export interface GetBooksIsbnRequestDto {
  pathParams: [{ name: 'isbn'; value: string }];
}

export type GetBooksIsbnResponseDto = ResponseDto<{
  myRecord: {
    myRating: number;
  };
  coverImageUrl: string;
  description: string;
  source: string;
  ratingCount: number;
  title: string;
  myRatingCount: number;
  bookId: number;
  purchaseUrl: string;
  replyCount: number;
  translators: undefined[];
  reviewCount: number;
  bookQuestion: {
    extract: string;
    question: string;
    plot: string;
  };
  averageRating: number;
  isbn13: string;
  totalPages: number;
  publisher: string;
  publishedDate: string;
  isRegisteredInMyLibrary: boolean;
  category: string;
  authors: undefined[];
  description: string;
  reviewCount: number;
  replyCount: number;
  myRatingCount: number;
  isRegisteredInMyLibrary: boolean;
  bookQuestion: {
    question: string | null;
    plot: string | null;
    extract: string | null;
  } | null;
}>;
