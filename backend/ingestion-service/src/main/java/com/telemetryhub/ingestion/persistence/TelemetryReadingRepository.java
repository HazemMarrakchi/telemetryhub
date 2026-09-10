package com.telemetryhub.ingestion.persistence;

import com.telemetryhub.ingestion.domain.TelemetryReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TelemetryReadingRepository extends JpaRepository<TelemetryReading, UUID> {

    @Query(value = """
            SELECT recorded_at as timestamp, value, metric
            FROM metrics.telemetry_readings
            WHERE tenant_id = :tenantId
              AND recorded_at BETWEEN :from AND :to
            ORDER BY recorded_at ASC
            """, nativeQuery = true)
    List<MetricPoint> findRawRange(@Param("tenantId") UUID tenantId,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);

    @Query(value = """
            SELECT recorded_at as timestamp, value, metric
            FROM metrics.telemetry_readings
            WHERE tenant_id = :tenantId
              AND equipment_id = :equipmentId
              AND recorded_at BETWEEN :from AND :to
            ORDER BY recorded_at ASC
            """, nativeQuery = true)
    List<MetricPoint> findRawRangeByEquipment(@Param("tenantId") UUID tenantId,
                                              @Param("equipmentId") UUID equipmentId,
                                              @Param("from") Instant from,
                                              @Param("to") Instant to);

    @Query(value = """
            SELECT bucket_ts AS timestamp, avg(value) AS value, metric
            FROM (
                SELECT time_bucket(CAST(:bucket AS interval), recorded_at) AS bucket_ts,
                       value,
                       metric
                FROM metrics.telemetry_readings
                WHERE tenant_id = :tenantId
                  AND recorded_at BETWEEN :from AND :to
            ) t
            GROUP BY bucket_ts, metric
            ORDER BY bucket_ts ASC
            """, nativeQuery = true)
    List<MetricPoint> findAggregated(@Param("tenantId") UUID tenantId,
                                     @Param("bucket") String bucket,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to);

    @Query(value = """
            SELECT bucket_ts AS timestamp, avg(value) AS value, metric
            FROM (
                SELECT time_bucket(CAST(:bucket AS interval), recorded_at) AS bucket_ts,
                       value,
                       metric
                FROM metrics.telemetry_readings
                WHERE tenant_id = :tenantId
                  AND equipment_id = :equipmentId
                  AND recorded_at BETWEEN :from AND :to
            ) t
            GROUP BY bucket_ts, metric
            ORDER BY bucket_ts ASC
            """, nativeQuery = true)
    List<MetricPoint> findAggregatedByEquipment(@Param("tenantId") UUID tenantId,
                                                @Param("equipmentId") UUID equipmentId,
                                                @Param("bucket") String bucket,
                                                @Param("from") Instant from,
                                                @Param("to") Instant to);

    @Query(value = """
            SELECT time_bucket('5 minutes', recorded_at) AS timestamp,
                   avg(value) AS value,
                   metric
            FROM metrics.telemetry_readings
            WHERE tenant_id = :tenantId
              AND recorded_at >= now() - interval '30 minutes'
            GROUP BY time_bucket('5 minutes', recorded_at), metric
            ORDER BY timestamp DESC
            """, nativeQuery = true)
    List<Object[]> findLatest(@Param("tenantId") UUID tenantId, org.springframework.data.domain.Pageable pageable);
}