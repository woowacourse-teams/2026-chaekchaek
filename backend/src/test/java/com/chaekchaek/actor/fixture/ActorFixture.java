package com.chaekchaek.actor.fixture;

import com.chaekchaek.actor.domain.Actor;
import org.springframework.test.util.ReflectionTestUtils;

public final class ActorFixture {

    private ActorFixture() {
    }

    public static Actor savedActor(long id, Actor actor) {
        ReflectionTestUtils.setField(actor, "id", id);
        return actor;
    }
}
