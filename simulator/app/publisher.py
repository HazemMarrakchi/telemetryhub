from __future__ import annotations

import json
from collections.abc import Callable

from kafka import KafkaProducer


class TelemetryPublisher:
    """Publie les lectures telemetriques sur la topic Kafka telemetry.raw.

    Le producer JSON est injectable pour permettre les tests unitaires.
    """

    def __init__(
        self,
        bootstrap_servers: str,
        topic: str,
        producer_factory: Callable[..., object] | None = None,
    ):
        self.topic = topic
        if producer_factory is not None:
            self._producer = producer_factory()
        else:
            self._producer = KafkaProducer(
                bootstrap_servers=bootstrap_servers,
                value_serializer=lambda v: json.dumps(v).encode("utf-8"),
                key_serializer=lambda k: k.encode("utf-8"),
                acks="all",
                retries=5,
                linger_ms=10,
                batch_size=16384,
            )

    def send_batch(self, readings: list[dict]) -> None:
        for reading in readings:
            self._producer.send(
                self.topic,
                key=reading["equipmentId"],
                value=reading,
            )

    def flush(self) -> None:
        flush = getattr(self._producer, "flush", None)
        if callable(flush):
            flush()

    def close(self) -> None:
        close = getattr(self._producer, "close", None)
        if callable(close):
            close()
