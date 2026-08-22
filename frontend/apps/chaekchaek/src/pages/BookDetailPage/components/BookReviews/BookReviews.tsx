import {
  Avatar,
  Button,
  Entry,
  Note,
  SegmentedControl,
  Select,
  Shell,
  Surface,
  Title,
} from '@chaekchaek/design-system';

import { useExecute } from '@/services/core/useExecute';
import {
  postReviewsReviewIdReactions,
  deleteReviewsReviewIdReactions,
} from '@/services/apis/reviewsReviewIdReactions/repository';

import type { BookReviewsProps } from './BookReviews.types';

export const BookReviews = ({
  sort,
  feed,
  count,
  reviews,
  onSortChange,
  onFeedChange,
}: BookReviewsProps) => {
  const { mutate: postReactionMutate } = useExecute({ executeFn: postReviewsReviewIdReactions });
  const { mutate: deleteReactionMutate } = useExecute({
    executeFn: deleteReviewsReviewIdReactions,
  });

  const handleClickReaction = async ({
    reviewId,
    likedByMe,
  }: {
    reviewId: number;
    likedByMe: boolean;
  }) => {
    if (!likedByMe) return await postReactionMutate({ reviewId });
    await deleteReactionMutate({ reviewId });
  };

  return (
    <>
      <Title
        level="main"
        trailing={
          <>
            <Select
              value={sort}
              options={[
                { value: 'LATEST', text: '최신순' },
                { value: 'OLDEST', text: '오래된순' },
                { value: 'POPULAR', text: '인기순' },
                { value: 'PAGE', text: '페이지순' },
              ]}
              onChange={onSortChange}
            />
            <SegmentedControl
              value={feed}
              options={[
                { value: 'ALL', text: '전체 피드' },
                { value: 'MINE', text: '내 피드' },
              ]}
              onChange={onFeedChange}
            />
          </>
        }
      >
        이 책에 남긴 감상 {count}
      </Title>
      {reviews?.map((review) => {
        return (
          <Entry key={review.reviewId} variant={review.deleted ? 'subtle' : 'plain'}>
            <Entry.Main>
              <Entry.Header>
                <Shell>
                  <Shell.Leading>
                    <Avatar img={review.author.profileImageUrl} />
                  </Shell.Leading>
                  <Shell.Content
                    title={review.author.displayName}
                    content={new Date(review.createdAt).toLocaleDateString('ko-KR')}
                  />
                  <Shell.Trailing>Trailing</Shell.Trailing>
                </Shell>
              </Entry.Header>
              <Entry.Body>
                {review.content}
                {review.quote && <Note>{review.quote}</Note>}
              </Entry.Body>
              <Entry.Footer>
                <Button
                  size="small"
                  leading={review.likedByMe ? '♥' : '♡'}
                  onClick={() => {
                    handleClickReaction({ reviewId: review.reviewId, likedByMe: review.likedByMe });
                  }}
                >
                  좋아요 {review.likeCount}
                </Button>
                <Button size="small" leading={'💬'}>
                  답글 {review.replyCount}
                </Button>
              </Entry.Footer>
            </Entry.Main>
            {review.recentReplies.length > 0 && (
              <Entry.Extension>
                {review.recentReplies.map((recentReply) => {
                  return (
                    <Surface key={recentReply.replyId}>
                      <Shell>
                        <Shell.Leading>
                          <Avatar img={recentReply.author.profileImageUrl} size="small" />
                        </Shell.Leading>
                        <Shell.Content
                          title={recentReply.author.displayName}
                          description={recentReply.content}
                        />
                        <Shell.Trailing>
                          {recentReply.likedByMe ? '♥' : '♡'}
                          {recentReply.likeCount}
                        </Shell.Trailing>
                      </Shell>
                    </Surface>
                  );
                })}
              </Entry.Extension>
            )}
          </Entry>
        );
      })}
    </>
  );
};
