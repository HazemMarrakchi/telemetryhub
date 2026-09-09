from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from typing import Annotated

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from app.anomaly import ZScoreDetector
from app.db import recent_anomalies, save_anomalies
from app.kafka_consumer import AnomalyStreamConsumer
from app.kb import KnowledgeBase
from app.rag import AssistantResponder
from app.settings import settings

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-7s %(name)s - %(message)s",
)
logger = logging.getLogger("ai.main")

kb = KnowledgeBase()
responder = AssistantResponder()
consumer = AnomalyStreamConsumer()


@asynccontextmanager
async def lifespan(app: FastAPI):
    ingested = 0
    if settings.db_enabled:
        try:
            ingested = kb.ingest_markdown_dir(settings.kb_path)
        except Exception:
            logger.warning("Base de connaissances non ingérée en startup", exc_info=True)
    logger.info(
        "ai-service prêt (documents KB=%d, db=%s, kafka=%s)",
        ingested, settings.db_enabled, settings.kafka_enabled,
    )
    consumer.start()
    yield
    consumer.stop()


app = FastAPI(title="TelemetryHub AI Service", version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class ReadingPayload(BaseModel):
    equipmentId: str
    tenantId: str
    metric: str
    value: float
    unit: str | None = None
    recordedAt: str = Field(..., description="Timestamp ISO 8601")
    source: str = "API"
    deviceFingerprint: str | None = None


class DetectRequest(BaseModel):
    tenantId: str
    readings: list[ReadingPayload]


class ChatRequest(BaseModel):
    question: str = Field(..., max_length=2000)
    tenantId: str = "11111111-1111-1111-1111-111111111111"


@app.get("/api/v1/ai/health")
def health() -> dict:
    return {
        "status": "ok",
        "service": settings.app_name,
        "version": "1.0.0",
        "dbEnabled": settings.db_enabled,
        "kafkaEnabled": settings.kafka_enabled,
        "documents": kb.count(),
    }


@app.post("/api/v1/ai/detect")
def detect(request: DetectRequest) -> dict:
    detector = ZScoreDetector(window_size=settings.window_size, z_threshold=settings.z_threshold)
    anomalies = []
    for reading in request.readings:
        anomaly = detector.analyze(reading.__dict__)
        if anomaly is not None:
            anomalies.append(anomaly.to_dict())
    if anomalies and settings.db_enabled:
        try:
            save_anomalies(anomalies)
        except Exception:
            logger.warning("Anomalies non persistées", exc_info=True)
    return {"tenantId": request.tenantId, "anomalies": anomalies, "count": len(anomalies)}


@app.get("/api/v1/ai/anomalies")
def list_anomalies(
    tenant_id: Annotated[str, Query(alias="tenantId")],
    limit: int = Query(default=100, ge=1, le=500),
) -> dict:
    if not settings.db_enabled:
        raise HTTPException(status_code=503, detail="Persistance désactivée")
    return {"tenantId": tenant_id, "anomalies": recent_anomalies(tenant_id, limit)}


@app.post("/api/v1/assistant/chat")
def chat(request: ChatRequest) -> dict:
    hits = kb.retrieve(request.question, request.tenantId)
    return responder.answer(request.question, hits)


@app.get("/api/v1/assistant/knowledge/stats")
def kb_stats() -> dict:
    return {"documents": kb.count(), "topK": kb.top_k, "embedder": kb.embedder.__class__.__name__}
