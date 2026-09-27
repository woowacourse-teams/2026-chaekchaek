package com.chaekchaek.auth.token.guest;

import com.chaekchaek.common.auth.ActorType;
import java.time.LocalDateTime;

public record IssuedGuestToken(String value, String nickname, LocalDateTime expiresAt,
                               long actorId, ActorType actorType) {
}
