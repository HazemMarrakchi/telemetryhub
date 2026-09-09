from __future__ import annotations

import logging
import re
from pathlib import Path

from sqlalchemy import text

from app.db import get_engine, session
from app.embedder import Embedder, HashingEmbedder
from app.settings import settings

logger = logging.getLogger("ai.kb")

_SECTION_RE = re.compile(r"^##\s+(.+)$", re.MULTILINE)


class KnowledgeBase:
    """Catalogue des fiches equipements ingerees dans pgvector.

    Les sections markdown ``## Titre`` deviennent des documents uniques.
    La recherche cosinus est effectuee via l'operateur pgvector ``<=>``.
    """

    def __init__(self, embedder: Embedder | None = None, top_k: int | None = None):
        self.embedder = embedder or HashingEmbedder()
        self.top_k = top_k or settings.rag_top_k

    def ingest_markdown_dir(self, kb_path: str, tenant_id: str = "*") -> int:
        if not settings.db_enabled:
            logger.warning("KB non ingérée: base de données désactivée")
            return 0
        directory = Path(kb_path)
        total = 0
        for markdown_file in sorted(directory.glob("*.md")):
            total += self._ingest_file(markdown_file, tenant_id)
        logger.info("KB ingérée: %d documents depuis %s", total, directory)
        return total

    def _ingest_file(self, markdown_file: Path, tenant_id: str) -> int:
        text_content = markdown_file.read_text(encoding="utf-8")
        sections = _split_sections(text_content)
        inserted = 0
        with session() as db:
            for title, content in sections:
                embedding = self.embedder.embed(title + " " + content)
                db.execute(
                    text(
                        "INSERT INTO ai.documents (id, tenant_id, title, source, content, embedding, created_at) "
                        "VALUES (gen_random_uuid(), :tid, :title, :source, :content, :emb, now()) "
                        "ON CONFLICT DO NOTHING"
                    ),
                    {
                        "tid": tenant_id,
                        "title": title,
                        "source": markdown_file.name,
                        "content": content,
                        "emb": "[" + ",".join(f"{v:.6f}" for v in embedding) + "]",
                    },
                )
                inserted += 1
            db.commit()
        return inserted

    def retrieve(self, query: str, tenant_id: str) -> list[dict]:
        engine = get_engine()
        if engine is None:
            return []
        embedding = self.embedder.embed(query)
        emb_sql = "[" + ",".join(f"{v:.6f}" for v in embedding) + "]"
        params = {"tid": tenant_id, "emb": emb_sql, "k": self.top_k, "dim": 128}
        with engine.connect() as conn:
            rows = conn.execute(
                text(
                    "SELECT title, source, content, "
                    "1 - (embedding <=> CAST(:emb AS vector)) AS score "
                    "FROM ai.documents "
                    "WHERE (tenant_id = :tid OR tenant_id = '*') "
                    "ORDER BY embedding <=> CAST(:emb AS vector) "
                    "LIMIT :k"
                ),
                params,
            ).mappings().all()
        return [
            {
                "title": row["title"],
                "source": row["source"],
                "content": row["content"],
                "score": round(float(row["score"]), 4),
            }
            for row in rows
        ]

    def count(self) -> int:
        if not settings.db_enabled:
            return 0
        try:
            with session() as db:
                return int(db.execute(text("SELECT count(*) FROM ai.documents")).scalar())
        except Exception:
            return 0


def _split_sections(markdown: str) -> list[tuple[str, str]]:
    matches = list(_SECTION_RE.finditer(markdown))
    if not matches:
        return [("Document", markdown.strip()[:2000])]
    sections: list[tuple[str, str]] = []
    for idx, match in enumerate(matches):
        start = match.end()
        end = matches[idx + 1].start() if idx + 1 < len(matches) else len(markdown)
        content = markdown[start:end].strip()
        if content:
            sections.append((match.group(1).strip(), content[:2000]))
    return sections
