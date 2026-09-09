from __future__ import annotations

import textwrap


class AssistantResponder:
    """Compose une reponse ancree sur les documents recuperes.

    Aucune generation libre : la reponse cite les extraits pertinents et
    indique clairement leurs sources. Si aucune source n'est pertinente,
    le systeme le dit explicitement plutot que de produire du contenu inventé.
    """

    MAX_SNIPPET_CHARS = 900

    def answer(self, question: str, hits: list[dict]) -> dict:
        if not hits:
            return {
                "answer": (
                    "Je n'ai pas trouvé de fiche technique pertinente dans la base de "
                    "connaissances pour répondre à cette question. Reformulez-la ou "
                    "précisez la métrique ou le type d'équipement concerné."
                ),
                "sources": [],
            }

        best = hits[0]
        snippets = []
        for hit in hits[:3]:
            content = textwrap.shorten(hit["content"], width=self.MAX_SNIPPET_CHARS, placeholder="…")
            snippets.append(
                {
                    "title": hit["title"],
                    "source": hit["source"],
                    "content": content,
                    "score": hit["score"],
                }
            )

        intro = (
            f"Voici les éléments de la base de connaissances les plus proches de votre "
            f"question « {question[:120]} »."
        )
        main = f"Élément le plus pertinent — {best['title']} (source: {best['source']})."
        return {
            "answer": f"{intro}\n{main}",
            "sources": snippets,
        }
