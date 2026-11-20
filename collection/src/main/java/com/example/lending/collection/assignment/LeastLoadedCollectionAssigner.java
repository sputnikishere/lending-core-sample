package com.example.lending.collection.assignment;

import com.example.lending.collection.domain.CollectionApplication;
import com.example.lending.collection.enums.CollectionStatusEnum;
import com.example.lending.collection.model.CollectorData;
import com.example.lending.collection.repository.CollectionApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Least-loaded assignment: a case goes to the collector with the fewest open cases;
 * on equal load - to the first by login, so the order is reproducible.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeastLoadedCollectionAssigner implements ICollectionAssigner {

    private final ICollectorRoster collectorRoster;
    private final CollectionApplicationRepository collectionApplicationRepository;

    @Override
    public boolean assign(CollectionApplication application) {
        List<CollectorData> collectors = collectorRoster.collectors();
        if (collectors.isEmpty()) {
            collectorRoster.invalidate();
            log.warn("Collection case for loan {} not assigned - no active collectors",
                    application.getCreditId());
            return false;
        }

        Map<UUID, Long> load = currentLoad();

        Optional<CollectorData> target = collectors.stream()
                .min(Comparator
                        .comparingLong((CollectorData collector) -> load.getOrDefault(collector.id(), 0L))
                        .thenComparing(CollectorData::login));

        target.ifPresent(collector ->
                application.assignTo(collector.id(), collector.login(), LocalDateTime.now()));

        return target.isPresent();
    }

    /**
     * Current collector load - the number of open cases per employee.
     */
    private Map<UUID, Long> currentLoad() {
        Set<CollectionStatusEnum> terminal = Arrays.stream(CollectionStatusEnum.values())
                .filter(CollectionStatusEnum::isTerminal)
                .collect(Collectors.toSet());

        Map<UUID, Long> load = new HashMap<>();
        for (Object[] row : collectionApplicationRepository.countActiveByAssignee(terminal)) {
            load.put((UUID) row[0], (Long) row[1]);
        }
        return load;
    }
}
