from __future__ import annotations

import json
import logging
import threading

from app.anomaly import ZScoreDetector
from app.db import save_anomalies
from app.settings import settings

logger = logging.getLogger("ai.kafka")

try:
    from kafka import KafkaConsumer, KafkaProducer

    KAFKA_AVAILABLE = True
except ImportError:  # pragma: no cover
    KAFKA_AVAILABLE = False


class AnomalyStreamConsumer:
    """Consomme telemetry.raw, detecte les anomalies et publie sur
    telemetry.anomaly + persist l'historique."""

    def __init__(self, detector: ZScoreDetector | None = None):
        self.detector = detector or ZScoreDetector(
            window_size=settings.window_size, z_threshold=settings.z_threshold
        )
        self._thread: threading.Thread | None = None
        self._stop = threading.Event()

    def start(self) -> None:
        if not settings.kafka_enabled or not KAFKA_AVAILABLE:
            logger.warning("Consommateur Kafka désactivé")
            return
        self._thread = threading.Thread(target=self._run, name="ai-anomaly-stream", daemon=True)
        self._thread.start()
        logger.info("Consommateur d'anomalies démarré")

    def stop(self) -> None:
        self._stop.set()
        if self._thread:
            self._thread.join(timeout=2)

    def _run(self) -> None:
        consumer = KafkaConsumer(
            settings.raw_topic,
            bootstrap_servers=settings.kafka_bootstrap_servers,
            group_id=settings.consumer_group,
            auto_offset_reset="earliest",
            value_deserializer=lambda b: json.loads(b.decode("utf-8")),
        )
        producer = KafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
            key_serializer=lambda k: k.encode("utf-8"),
            acks="all",
        )
        try:
            for message in consumer:
                if self._stop.is_set():
                    break
                reading = message.value
                anomaly = self.detector.analyze(reading)
                if anomaly is not None:
                    payload = anomaly.to_dict()
                    producer.send(settings.anomaly_topic, key=anomaly.equipment_id, value=payload)
                    producer.flush()
                    try:
                        save_anomalies([payload])
                    except Exception:
                        logger.warning("Anomalie détectée mais non persistée", exc_info=True)
                    logger.warning(
                        "Anomalie [%s] %s %s=%.2f (z=%.2f)",
                        anomaly.severity, anomaly.equipment_id, anomaly.metric,
                        anomaly.value, anomaly.z_score,
                    )
        finally:
            consumer.close()
            producer.close()
