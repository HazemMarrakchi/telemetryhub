from __future__ import annotations

import logging
import uuid
from datetime import UTC, datetime
from typing import ClassVar

from pgvector.sqlalchemy import Vector
from sqlalchemy import Engine, UniqueConstraint, create_engine, text
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column

from app.settings import settings

logger = logging.getLogger("ai.db")


class Base(DeclarativeBase):
    pass


class Document(Base):
    __tablename__ = "documents"
    __table_args__ = (
        UniqueConstraint("tenant_id", "title", "source", name="uq_documents_tenant_title_source"),
        {"schema": "ai"},
    )

    id: Mapped[str] = mapped_column(primary_key=True, default=lambda: str(uuid.uuid4()))
    tenant_id: Mapped[str] = mapped_column(nullable=False, index=True)
    title: Mapped[str] = mapped_column(nullable=False)
    source: Mapped[str] = mapped_column(nullable=False)
    content: Mapped[str] = mapped_column(nullable=False)
    embedding: Mapped[list[float]] = mapped_column(Vector(128))
    created_at: Mapped[str] = mapped_column(
        default=lambda: datetime.now(UTC).isoformat(timespec="milliseconds")
    )


class AnomalyEvent(Base):
    __tablename__ = "anomaly_events"
    __table_args__: ClassVar[dict] = {"schema": "ai"}

    id: Mapped[str] = mapped_column(primary_key=True, default=lambda: str(uuid.uuid4()))
    tenant_id: Mapped[str] = mapped_column(nullable=False, index=True)
    equipment_id: Mapped[str] = mapped_column(nullable=False, index=True)
    metric: Mapped[str] = mapped_column(nullable=False)
    value: Mapped[float] = mapped_column(nullable=False)
    z_score: Mapped[float] = mapped_column(nullable=False)
    severity: Mapped[str] = mapped_column(nullable=False)
    recorded_at: Mapped[str] = mapped_column(nullable=False)
    created_at: Mapped[str] = mapped_column(
        default=lambda: datetime.now(UTC).isoformat(timespec="milliseconds")
    )


_engine: Engine | None = None


def get_engine() -> Engine | None:
    global _engine
    if _engine is None and settings.db_enabled:
        engine = create_engine(settings.database_url, pool_pre_ping=True)
        _ensure_schema(engine)
        Base.metadata.create_all(engine)
        logger.info("Base de donnees ai initialisee")
        _engine = engine
    return _engine


def _ensure_schema(engine: Engine) -> None:
    with engine.begin() as conn:
        conn.execute(text("CREATE SCHEMA IF NOT EXISTS ai"))
        try:
            conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))
        except Exception:
            logger.warning("Extension pgvector indisponible; stockage vectoriel désactivé", exc_info=False)


def session() -> Session:
    engine = get_engine()
    if engine is None:
        raise RuntimeError("Base de donnees désactivée")
    return Session(engine)


def save_anomalies(anomalies: list[dict]) -> None:
    if not anomalies:
        return
    try:
        with session() as db:
            for anomaly in anomalies:
                db.add(AnomalyEvent(
                    tenant_id=anomaly["tenantId"],
                    equipment_id=anomaly["equipmentId"],
                    metric=anomaly["metric"],
                    value=anomaly["value"],
                    z_score=anomaly["zScore"],
                    severity=anomaly["severity"],
                    recorded_at=anomaly["recordedAt"],
                ))
            db.commit()
    except Exception:
        logger.exception("Échec de persistance des anomalies")
        raise


def recent_anomalies(tenant_id: str, limit: int = 100) -> list[dict]:
    if not settings.db_enabled:
        return []
    with session() as db:
        rows = db.execute(
            text(
                "SELECT id, tenant_id, equipment_id, metric, value, z_score, severity, recorded_at "
                "FROM ai.anomaly_events "
                "WHERE tenant_id = :tid "
                "ORDER BY recorded_at DESC LIMIT :limit"
            ),
            {"tid": tenant_id, "limit": limit},
        ).mappings().all()
    return [dict(row) for row in rows]
