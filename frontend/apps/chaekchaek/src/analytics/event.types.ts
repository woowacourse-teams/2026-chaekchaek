export type AnalyticsEventMap = {
  select_book: {
    source: 'intro_popular' | 'search';
  };
  rating_open: undefined;
  rating_submit: undefined;
  rating_select: undefined;
  library_add: {
    source: 'search' | 'detail_info' | 'detail_required';
    status: 'want_to_read' | 'reading' | 'finished';
  };
  review_write_open: {
    user_type: 'member' | 'guest';
  };
  review_submit: {
    user_type: 'member' | 'guest';
  };
};
