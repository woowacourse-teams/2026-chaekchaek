import type { ResponseDto } from '@/services/apis/api.types';

export interface PostBooksByIsbnIsbn13ReviewsRequestDto {
  pathParams: [{ name: 'isbn13'; value: string }];
  data: {
    chapter: string;
    isSpoiler: boolean;
    quote: string;
    totalPages: number;
    currentPage: number;
    content: string;
  };
}

export type PostBooksByIsbnIsbn13ReviewsResponseDto = ResponseDto<undefined>;
