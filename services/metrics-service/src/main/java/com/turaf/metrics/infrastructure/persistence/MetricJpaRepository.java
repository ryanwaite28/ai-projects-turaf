package com.turaf.metrics.infrastructure.persistence;

import com.turaf.metrics.domain.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MetricJpaRepository extends JpaRepository<MetricJpaEntity, String> {

    List<MetricJpaEntity> findByExperimentIdAndTimestampBetweenOrderByTimestampDesc(
        String experimentId, Instant start, Instant end);

    List<MetricJpaEntity> findByExperimentIdAndNameAndTimestampBetweenOrderByTimestampDesc(
        String experimentId, String name, Instant start, Instant end);

    List<MetricJpaEntity> findByOrganizationIdAndTimestampBetweenOrderByTimestampDesc(
        String organizationId, Instant start, Instant end);

    @Query("SELECT AVG(m.value) FROM MetricJpaEntity m WHERE m.experimentId = :experimentId AND m.name = :name")
    Double calculateAverage(@Param("experimentId") String experimentId, @Param("name") String name);

    @Query("SELECT MIN(m.value) FROM MetricJpaEntity m WHERE m.experimentId = :experimentId AND m.name = :name")
    Double calculateMin(@Param("experimentId") String experimentId, @Param("name") String name);

    @Query("SELECT MAX(m.value) FROM MetricJpaEntity m WHERE m.experimentId = :experimentId AND m.name = :name")
    Double calculateMax(@Param("experimentId") String experimentId, @Param("name") String name);

    @Query("SELECT SUM(m.value) FROM MetricJpaEntity m WHERE m.experimentId = :experimentId AND m.name = :name")
    Double calculateSum(@Param("experimentId") String experimentId, @Param("name") String name);

    @Query("SELECT COUNT(m) FROM MetricJpaEntity m WHERE m.experimentId = :experimentId AND m.name = :name")
    Long countByExperimentIdAndName(@Param("experimentId") String experimentId, @Param("name") String name);

    void deleteByExperimentId(String experimentId);
}
