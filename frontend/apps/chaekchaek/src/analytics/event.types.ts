export type AnalyticsEventMap = {
  navigate: {
    destination: 'search' | 'book_detail' | 'members_library' | 'libraries' | 'library' | 'feed';
    source:
      | 'intro_popular'
      | 'intro_latest_reviews'
      | 'search'
      | 'book_reviews'
      | 'book_feed'
      | 'libraries'
      | 'library'
      | 'header';
  };
  rating_open: undefined;
  rating_submit: undefined;
  rating_select: undefined;
  library_add: {
    source: 'search' | 'detail_info' | 'detail_required' | 'library' | 'librareis';
    status: 'want_to_read' | 'reading' | 'finished';
  };
  library_add_open: {
    source: 'search' | 'detail_info' | 'library';
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
  current_page_open: undefined;
  current_page_submit: undefined;
  search: {
    query: string;
  };
  search_more: {
    page: number;
    query: string;
  };
};
