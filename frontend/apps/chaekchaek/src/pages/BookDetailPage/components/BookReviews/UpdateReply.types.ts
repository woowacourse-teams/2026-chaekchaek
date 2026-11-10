type Reply = {
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
};

export type UpdateReplyProps = {
  reply: Reply;
  onReplyUpdated: () => void | Promise<void>;
};
