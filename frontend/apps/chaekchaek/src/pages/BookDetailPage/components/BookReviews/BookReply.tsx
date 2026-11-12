import { useState } from 'react';
import { generatePath, Link } from 'react-router-dom';

import {
  Avatar,
  Button,
  DropdownMenu,
  Icon,
  IconButton,
  Shell,
  Surface,
} from '@chaekchaek/design-system';

import { ROUTES } from '@/constants/routes';
import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { useExecute } from '@/services/core/useExecute';
import {
  deleteRepliesReplyIdReactions,
  postRepliesReplyIdReactions,
} from '@/services/apis/repliesReplyIdReactions/repository';

import { deleteRepliesReplyId } from '@/services/apis/repliesReplyId/repository';

import { track } from '@/analytics/track';

import { UpdateReply } from './UpdateReply';

import type { BookReplyProps } from './BookReply.types';

const SPOILER_PLACEHOLDER_REPLY = '“짹짹짹 짹짹 짹짹짹짹 짹짹.”';

export const BookReply = (props: BookReplyProps) => {
  const { isbn, reply, showSpoilerVisible, onShowSpoiler, onRefetchGetReplies, onClickAvatar } =
    props;

  const { isAuthenticated, user, guest } = useAuthContext();

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

    if (user) {
      track('reply_like', {
        value: !likedByMe,
        actor_type: user.actorType,
        actor_id: user.actorId,
      });
    }
    if (guest) {
      track('reply_like', {
        value: !likedByMe,
        actor_type: guest.actorType,
        actor_id: guest.actorId,
      });
    }
    await onRefetchGetReplies();
  };

  const [isEditing, setSetIsEditing] = useState(false);
  const handleStartEdit = () => {
    setSetIsEditing(true);

    const actorType = user?.actorType ?? guest?.actorType;
    const actorId = user?.actorId ?? guest?.actorId;

    if (!(actorType && actorId)) return;
    track('reply_update_open', {
      actor_type: actorType,
      actor_id: actorId,
      isbn,
    });
  };

  const { mutate: deleteReply } = useExecute({
    executeFn: deleteRepliesReplyId,
  });

  const handleClickDeleteReply = async (replyId: number) => {
    await deleteReply({ replyId });

    const actorType = user?.actorType ?? guest?.actorType;
    const actorId = user?.actorId ?? guest?.actorId;

    if (!(actorType && actorId)) return;
    track('reply_delete', {
      actor_type: actorType,
      actor_id: actorId,
      isbn,
    });

    await onRefetchGetReplies();
  };

  if (isEditing)
    return <UpdateReply isbn={isbn} reply={reply} onReplyUpdated={onRefetchGetReplies} />;

  return (
    <Surface key={reply.replyId}>
      <Shell>
        <Shell.Leading>
          <Avatar
            img={reply.author.profileImageUrl}
            size="small"
            as={reply.author.memberId ? Link : 'div'}
            {...(reply.author.memberId && {
              to: generatePath(ROUTES.LEGACY_MEMBER_LIBRARY, {
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
            <DropdownMenu
              trigger={
                <IconButton>
                  <Icon.EllipsisIcon />
                </IconButton>
              }
              items={[
                { text: '수정', onClick: handleStartEdit },
                {
                  text: '삭제',
                  onClick: () => {
                    handleClickDeleteReply(reply.replyId);
                  },
                },
              ]}
            />
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
