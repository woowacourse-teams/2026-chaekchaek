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
  rating_submit: undefined;
  rating_select: undefined;
  library_add: {
    source: 'search' | 'detail_info' | 'detail_required' | 'library' | 'librareis';
  };
  library_remove: {
    source: 'search' | 'detail_info' | 'detail_required' | 'library' | 'librareis';
  };
  library_add_open: {
    source: 'search' | 'detail_info' | 'library';
  };
  library_management_open: undefined;
  library_register: undefined;
  library_register_open: undefined;
  library_delete: undefined;
  library_delete_open: undefined;
  library_rename: undefined;
  library_rename_open: undefined;
  library_save_image: undefined;
  library_save_image_open: undefined;
  library_start_edit: undefined;
  library_end_edit: undefined;
  library_reorder: undefined;
  library_reorder_book: undefined;
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
  review_update_open: {
    actor_type: 'MEMBER' | 'GUEST';
    actor_id: number;
    isbn: string;
  };
  review_update: {
    actor_type: 'MEMBER' | 'GUEST';
    actor_id: number;
    isbn: string;
  };
  review_delete: {
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
  reply_update_open: {
    actor_type: 'MEMBER' | 'GUEST';
    actor_id: number;
    isbn: string;
  };
  reply_update: {
    actor_type: 'MEMBER' | 'GUEST';
    actor_id: number;
    isbn: string;
  };
  reply_delete: {
    actor_type: 'MEMBER' | 'GUEST';
    actor_id: number;
    isbn: string;
  };
  review_like: {
    value: boolean;
    actor_type: 'MEMBER' | 'GUEST';
    actor_id: number;
  };
  reply_like: {
    value: boolean;
    actor_type: 'MEMBER' | 'GUEST';
    actor_id: number;
  };
  search: {
    query: string;
  };
  search_more: {
    page: number;
    query: string;
  };
};
