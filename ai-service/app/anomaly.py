from __future__ import annotations

import math
from collections.abc import Sequence
from dataclasses import dataclass


@dataclass
class Anomaly:
    equipment_id: str
    tenant_id: str
    metric: str
    value: float
    z_score: float
    mean: float
    stddev: float
    recorded_at: str
    severity: str

    def to_dict(self) -> dict:
        return {
            "equipmentId": self.equipment_id,
            "tenantId": self.tenant_id,
            "metric": self.metric,
            "value": self.value,
            "zScore": round(self.z_score, 3),
            "mean": round(self.mean, 3),
            "stddev": round(self.stddev, 3),
            "recordedAt": self.recorded_at,
            "severity": self.severity,
        }


class ZScoreDetector:
    """Detecteur statistique par metrique.

    Pour chaque (tenant, equipment, metric) on maintient une fenetre glissante
    (deque bornee). Une lecture est consideree anormale quand son score z
    depasse ``z_threshold`` ET que l'ecart-type de reference n'est pas nul.
    """

    def __init__(self, window_size: int = 240, z_threshold: float = 3.5):
        self.window_size = window_size
        self.z_threshold = z_threshold
        self._buffers: dict[tuple[str, str, str], list[float]] = {}

    def reset(self) -> None:
        self._buffers.clear()

    def analyze(self, reading: dict) -> Anomaly | None:
        key = (reading["tenantId"], reading["equipmentId"], reading["metric"])
        value = float(reading["value"])
        buf = self._buffers.setdefault(key, [])

        if len(buf) < max(20, self.window_size // 8):
            buf.append(value)
            return None

        mean, stddev = _rolling_stats(buf)
        z_score = 0.0 if stddev == 0.0 else (value - mean) / stddev

        buf.append(value)
        if len(buf) > self.window_size:
            buf.pop(0)

        if abs(z_score) >= self.z_threshold:
            return Anomaly(
                equipment_id=reading["equipmentId"],
                tenant_id=reading["tenantId"],
                metric=reading["metric"],
                value=value,
                z_score=z_score,
                mean=mean,
                stddev=stddev,
                recorded_at=reading["recordedAt"],
                severity=_severity(abs(z_score)),
            )
        return None


def _rolling_stats(values: Sequence[float]) -> tuple[float, float]:
    n = len(values)
    mean = sum(values) / n
    variance = sum((v - mean) ** 2 for v in values) / n
    return mean, math.sqrt(variance)


def _severity(abs_z: float) -> str:
    if abs_z >= 6.0:
        return "CRITICAL"
    if abs_z >= 4.5:
        return "WARNING"
    return "INFO"
