type BookReply = {
  replyId: number;
  author: {
    memberId?: number | null;
    actorType?: 'MEMBER' | 'GUEST';
    profileStatus?: 'AVAILABLE' | 'UNAVAILABLE' | 'WITHDRAWN';
    displayName: string;
    profileImageUrl: string;
    mine: boolean;
  };
  createdAt: string;
  content: string;
  likedByMe: boolean;
  likeCount: number;
  deleted: boolean;
};

export type BookReplyProps = {
  isbn: string;
  reply: BookReply;
  showSpoilerVisible: boolean;
  onShowSpoiler: () => void;
  onRefetchGetReplies: () => void;
  onClickAvatar: (isProfileAvailable: boolean) => void;
};
