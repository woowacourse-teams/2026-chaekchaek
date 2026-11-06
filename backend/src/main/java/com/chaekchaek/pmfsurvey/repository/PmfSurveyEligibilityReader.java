package com.chaekchaek.pmfsurvey.repository;

import java.time.Instant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PmfSurveyEligibilityReader extends Repository<com.chaekchaek.pmfsurvey.domain.PmfSurveyResponse, Long> {

    @Query(value = """
            select exists(
                select 1 from review r
                where r.actor_id = :actorId and r.created_at >= :since
            ) or exists(
                select 1 from reply rp
                where rp.actor_id = :actorId and rp.created_at >= :since
            ) or exists(
                select 1 from reaction re
                where re.actor_id = :actorId and re.created_at >= :since
            ) or exists(
                select 1 from reply_reaction rr
                where rr.actor_id = :actorId and rr.created_at >= :since
            ) or (
                :memberId is not null and exists(
                    select 1 from library_item li
                    where li.member_id = :memberId
                      and (li.added_at >= :since or li.rating_updated_at >= :since)
                )
            )
            """, nativeQuery = true)
    long hasCoreActionSince(
            @Param("actorId") long actorId,
            @Param("memberId") Long memberId,
            @Param("since") Instant since
    );
}
