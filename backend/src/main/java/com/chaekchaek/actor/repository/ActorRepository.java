package com.chaekchaek.actor.repository;

import com.chaekchaek.actor.domain.Actor;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ActorRepository extends JpaRepository<Actor, Long> {

    Optional<Actor> findByMemberId(long memberId);

    Optional<Actor> findByGuestTokenHash(String guestTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Actor a where a.id = :actorId")
    Optional<Actor> findByIdForUpdate(long actorId);

    @Modifying
    @Query(value = """
            insert into actor (member_id, actor_type, created_at)
            select member_id, 'MEMBER', created_at
            from member m
            where not exists (
                select 1 from actor a where a.member_id = m.member_id
            )
            """, nativeQuery = true)
    int backfillMissingMemberActors();
}
