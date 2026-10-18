import { useState } from 'react';
import { generatePath, Link } from 'react-router-dom';

import { Avatar, Button, Icon, Shell, Surface } from '@chaekchaek/design-system';

import { ROUTES } from '@/constants/routes';
import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { useExecute } from '@/services/core/useExecute';
import {
  deleteRepliesReplyIdReactions,
  postRepliesReplyIdReactions,
} from '@/services/apis/repliesReplyIdReactions/repository';

import { deleteRepliesReplyId } from '@/services/apis/repliesReplyId/repository';

import { UpdateReply } from './UpdateReply';

import type { BookReplyProps } from './BookReply.types';

const SPOILER_PLACEHOLDER_REPLY = '“짹짹짹 짹짹 짹짹짹짹 짹짹.”';

export const BookReply = (props: BookReplyProps) => {
  const { reply, showSpoilerVisible, onShowSpoiler, onRefetchGetReplies, onClickAvatar } = props;

  const { isAuthenticated } = useAuthContext();

  const { mutate: postReplyReactionMutate } = useExecute({
    executeFn: postRepliesReplyIdReactions,
  });
  const { mutate: deleteReplyReactionMutate } = useExecute({
    executeFn: deleteRepliesReplyIdReactions,
  });

  const handleClickReplyReaction = async ({
    replyId,
    likedByMe,
  }: {
    replyId: number;
    likedByMe: boolean;
  }) => {
    if (!likedByMe) {
      await postReplyReactionMutate({ replyId });
    } else {
      await deleteReplyReactionMutate({ replyId });
    }

    await onRefetchGetReplies();
  };

  const [isEditing, setSetIsEditing] = useState(false);
  const handleStartEdit = () => {
    setSetIsEditing(true);
  };

  const { mutate: deleteReply } = useExecute({
    executeFn: deleteRepliesReplyId,
  });

  const handleClickDeleteReply = async (replyId: number) => {
    await deleteReply({ replyId });

    await onRefetchGetReplies();
  };

  if (isEditing) return <UpdateReply reply={reply} onReplyUpdated={onRefetchGetReplies} />;

  return (
    <Surface key={reply.replyId}>
      <Shell>
        <Shell.Leading>
          <Avatar
            img={reply.author.profileImageUrl}
            size="small"
            as={reply.author.memberId ? Link : 'div'}
            {...(reply.author.memberId && {
              to: generatePath(ROUTES.MEMBER_LIBRARY, {
                memberId: reply.author.memberId.toString(),
              }),
            })}
            onClick={() => {
              onClickAvatar(reply.author.profileStatus !== 'AVAILABLE');
            }}
          />
        </Shell.Leading>
        <Shell.Content
          onClick={onShowSpoiler}
          title={reply.author.displayName}
          description={showSpoilerVisible ? reply.content : SPOILER_PLACEHOLDER_REPLY}
        />
        <Shell.Trailing>
          {reply.author.mine && (
            <>
              <Button shape="link" variant="ghost" size="x-small" onClick={handleStartEdit}>
                수정
              </Button>
              <Button
                shape="link"
                variant="ghost"
                size="x-small"
                onClick={() => {
                  handleClickDeleteReply(reply.replyId);
                }}
              >
                삭제
              </Button>
            </>
          )}

          <Button
            leading={reply.likedByMe ? <Icon.HeartOnIcon /> : <Icon.HeartOffIcon />}
            shape="link"
            variant="ghost"
            disabled={!isAuthenticated}
            onClick={() => {
              handleClickReplyReaction({
                replyId: reply.replyId,
                likedByMe: reply.likedByMe,
              });
            }}
          >
            {reply.likeCount}
          </Button>
        </Shell.Trailing>
      </Shell>
    </Surface>
  );
};
