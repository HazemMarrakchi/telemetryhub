from __future__ import annotations

import math
import random
from dataclasses import dataclass
from datetime import UTC


@dataclass(frozen=True)
class MetricProfile:
    """Profil nominal d'une metrique : valeur de base, amplitude de derive,
    bruit et comportement d'anomalie."""

    name: str
    unit: str
    base: float
    amplitude: float
    period_seconds: float
    noise: float
    spike_factor: float


DEFAULT_METRICS = (
    MetricProfile("temperature", "°C", 72.0, 2.5, 600.0, 0.4, 1.6),
    MetricProfile("vibration", "mm/s", 3.2, 0.8, 300.0, 0.1, 8.0),
    MetricProfile("pression", "bar", 6.0, 0.6, 900.0, 0.05, 1.8),
    MetricProfile("debit", "m3/h", 84.0, 4.0, 720.0, 1.0, 1.3),
    MetricProfile("puissance", "kW", 42.0, 3.0, 540.0, 0.8, 1.7),
    MetricProfile("humidite", "%", 45.0, 3.0, 1500.0, 0.7, 1.9),
)


def generate_reading(
    equipment_id: str,
    tenant_id: str,
    profile: MetricProfile,
    now: float,
    rng: random.Random,
    anomaly_rate: float = 0.0,
    source: str = "SIM",
    device_fingerprint: str | None = None,
) -> dict:
    """Genere une lecture telemetrique pour une metrique donnee.

    La valeur fluctue autour d'une base selon une derive sinusoidale lente
    plus un bruit. Si l'alerte `anomaly_rate` est depassee, la valeur subit
    un saut (spike) pour simuler un defaut capteur ou process.
    """
    phase = (now % profile.period_seconds) / profile.period_seconds * 2 * math.pi
    drift = profile.amplitude * math.sin(phase)
    noise = rng.gauss(0.0, profile.noise)
    value = profile.base + drift + noise

    if anomaly_rate > 0 and rng.random() < anomaly_rate:
        value *= profile.spike_factor

    return {
        "equipmentId": equipment_id,
        "tenantId": tenant_id,
        "metric": profile.name,
        "value": round(value, 3),
        "unit": profile.unit,
        "recordedAt": _iso_now(now),
        "source": source,
        "deviceFingerprint": device_fingerprint or f"{equipment_id}-sim",
    }


def generate_batch(
    equipment_id: str,
    tenant_id: str,
    now: float,
    rng: random.Random,
    metrics: tuple[MetricProfile, ...] = DEFAULT_METRICS,
    anomaly_rate: float = 0.0,
) -> list[dict]:
    """Genere un lot de lectures pour un equipement (toutes les metriques)."""
    return [
        generate_reading(equipment_id, tenant_id, profile, now, rng, anomaly_rate)
        for profile in metrics
    ]


def _iso_now(epoch_seconds: float) -> str:
    from datetime import datetime

    return datetime.fromtimestamp(epoch_seconds, tz=UTC).isoformat(timespec="milliseconds")
