from __future__ import annotations

import hashlib
import math
import re
from typing import Protocol

VECTOR_DIM = 128

_TOKEN_RE = re.compile(r"[a-z0-9_]+")


class Embedder(Protocol):
    dimension: int

    def embed(self, text: str) -> list[float]:
        """Retourne un vecteur normalise de `dimension` floats."""


class HashingEmbedder:
    """Embedder deterministe sans dependance externe.

    Chaque token est mappe par hachage sur une seed du vecteur et normalise
    (TF-like, l2). Suffisant pour la similarite cosine entre documents
    techniques courts, et utilisable hors-ligne en CI.
    """

    dimension = VECTOR_DIM

    def embed(self, text: str) -> list[float]:
        vector = [0.0] * self.dimension
        for token in _TOKEN_RE.findall(text.lower()):
            idx = int(hashlib.blake2b(token.encode("utf-8"), digest_size=4).hexdigest(), 16) % self.dimension
            vector[idx] += 1.0
        norm = math.sqrt(sum(v * v for v in vector)) or 1.0
        return [v / norm for v in vector]
