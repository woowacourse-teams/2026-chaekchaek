package com.chaekchaek.common.auth;

import java.util.Optional;

public interface CurrentActorProvider {

    CurrentActor getCurrentActor();

    default Optional<CurrentActor> findCurrentActor() {
        return Optional.empty();
    }
}
