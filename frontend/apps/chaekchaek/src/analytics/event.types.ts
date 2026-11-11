export type AnalyticsEventMap = {
  navigate: {
    destination: 'book_detail' | 'members_library' | 'libraries' | 'library';
    source:
      | 'intro_popular'
      | 'intro_latest_reviews'
      | 'search'
      | 'book_reviews'
      | 'book_feed'
      | 'libraries'
      | 'library';
  };
  rating_open: undefined;
  rating_submit: undefined;
  rating_select: undefined;
  library_add: {
    source: 'search' | 'detail_info' | 'detail_required' | 'library' | 'librareis';
  };
  review_write_open: {
    actor_type: 'MEMBER' | 'GUEST';
    actor_id: number;
    isbn: string;
  };
  review_submit: {
    actor_type: 'MEMBER' | 'GUEST';
    actor_id: number;
    isbn: string;
  };
  reply_write_open: {
    actor_type: 'MEMBER' | 'GUEST';
    actor_id: number;
    isbn: string;
  };
  reply_submit: {
    actor_type: 'MEMBER' | 'GUEST';
    actor_id: number;
    isbn: string;
  };
  search: undefined;
  search_more: {
    page: number;
  };
};
