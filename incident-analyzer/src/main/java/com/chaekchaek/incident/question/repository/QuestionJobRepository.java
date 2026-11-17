package com.chaekchaek.incident.question.repository;

import com.chaekchaek.incident.question.domain.QuestionJob;
import com.chaekchaek.incident.question.domain.QuestionState;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface QuestionJobRepository extends JpaRepository<QuestionJob, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QuestionJob q where q.id = :id")
    Optional<QuestionJob> lockById(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select q from QuestionJob q where q.environment in :environments and q.channel in :channels and (
                (q.workState in :ready and q.nextAttemptAt <= :now)
                or (q.workState = :processing and q.leaseUntil < :now)
            ) order by q.receivedAt, q.id
            """)
    List<QuestionJob> findAvailable(
            List<QuestionState> ready,
            QuestionState processing,
            Instant now,
            List<String> environments,
            List<String> channels,
            Pageable pageable
    );

    @Query("""
            select q from QuestionJob q where q.channel = :channel and q.threadTs = :threadTs
                and q.workState = :sent and q.id <> :currentId and q.receivedAt <= :receivedAt
            order by q.receivedAt desc, q.id desc
            """)
    List<QuestionJob> findConversation(
            String channel,
            String threadTs,
            QuestionState sent,
            String currentId,
            Instant receivedAt,
            Pageable pageable
    );
}
