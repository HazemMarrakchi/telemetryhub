from app.rag import AssistantResponder


def _hits():
    return [
        {
            "title": "Compresseur d'air à vis",
            "source": "equipments.md",
            "content": "Température de sortie nominale : 70 à 75 °C. Seuil d'alarme température : 85 °C.",
            "score": 0.91,
        },
        {
            "title": "Pompe centrifuge process",
            "source": "equipments.md",
            "content": "Cavitation à suspecter si vibrations, débit et pression varient simultanément.",
            "score": 0.64,
        },
    ]


def test_answer_returns_sources_and_intro():
    response = AssistantResponder().answer("Quel est le seuil de température ?", _hits())
    assert response["sources"]
    assert response["sources"][0]["title"] == "Compresseur d'air à vis"
    assert response["sources"][0]["source"] == "equipments.md"
    assert "seuil" in response["answer"].lower()


def test_answer_without_hits_is_honest():
    response = AssistantResponder().answer("Question exotique", [])
    assert response["sources"] == []
    assert "n'ai pas trouvé" in response["answer"]


def test_snippets_are_trimmed():
    long_content = "lorem ipsum " * 400
    hits = [{"title": "Doc", "source": "s.md", "content": long_content, "score": 0.9}]
    response = AssistantResponder().answer("long?", hits)
    assert len(response["sources"][0]["content"]) <= 920
