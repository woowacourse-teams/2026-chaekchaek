package com.chaekchaek.actor.service;

import com.chaekchaek.actor.repository.ActorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MemberActorBackfill implements ApplicationRunner {

    private final ActorRepository actorRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        actorRepository.backfillMissingMemberActors();
    }
}
