import type { ResponseDto } from '@/services/apis/api.types';

export interface PostBooksByIsbnIsbn13ReviewsRequestDto {
  pathParams: [{ name: 'isbn13'; value: string }];
  data: {
    chapter?: string | undefined;
    isSpoiler?: boolean | undefined;
    quote?: string | undefined;
    totalPages?: number | undefined;
    currentPage?: number | undefined;
    content: string;
  };
  headers: {
    'X-Guest-Token': string;
  };
}

export type PostBooksByIsbnIsbn13ReviewsResponseDto = ResponseDto<undefined>;
