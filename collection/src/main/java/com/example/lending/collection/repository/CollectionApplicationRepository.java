package com.example.lending.collection.repository;

import com.example.lending.collection.domain.CollectionApplication;
import com.example.lending.collection.enums.CollectionStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CollectionApplicationRepository extends JpaRepository<CollectionApplication, Long> {

    /**
     * Current collector load - the number of open cases per assignee.
     * Collectors with no cases are absent from the result; the caller treats them as zero load.
     *
     * @param statuses statuses considered finished, which do not create load
     * @return "collector id - case count" pairs
     */
    @Query("""
            select a.assigneeUserId, count(a)
            from CollectionApplication a
            where a.assigneeUserId is not null and a.status not in :statuses
            group by a.assigneeUserId
            """)
    List<Object[]> countActiveByAssignee(Collection<CollectionStatusEnum> statuses);
}
