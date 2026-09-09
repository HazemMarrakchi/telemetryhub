from __future__ import annotations

import os
from dataclasses import dataclass, field


@dataclass
class Settings:
    app_name: str = "ai-service"
    port: int = int(os.environ.get("AI_PORT", "8090"))

    database_url: str = os.environ.get(
        "DATABASE_URL", "postgresql+psycopg2://telemetryhub:telemetryhub-dev@localhost:5432/telemetryhub"
    )
    db_enabled: bool = os.environ.get("TELEMETRYHUB_DB_OFF", "0") != "1"

    kafka_bootstrap_servers: str = os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    kafka_enabled: bool = os.environ.get("TELEMETRYHUB_KAFKA_OFF", "0") != "1"
    raw_topic: str = os.environ.get("KAFKA_RAW_TOPIC", "telemetry.raw")
    anomaly_topic: str = os.environ.get("KAFKA_ANOMALY_TOPIC", "telemetry.anomaly")
    consumer_group: str = os.environ.get("KAFKA_CONSUMER_GROUP", "ai-service")

    z_threshold: float = float(os.environ.get("AI_Z_THRESHOLD", "3.5"))
    window_size: int = int(os.environ.get("AI_WINDOW_SIZE", "240"))
    rag_top_k: int = int(os.environ.get("AI_RAG_TOP_K", "5"))
    kb_path: str = os.environ.get("AI_KB_PATH", "knowledge")

    allowed_origins: list[str] = field(default_factory=lambda: ["http://localhost:4200"])


settings = Settings()
