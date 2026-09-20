from app.embedder import HashingEmbedder


def test_embedding_dimension_and_norm():
    embedder = HashingEmbedder()
    vector = embedder.embed("temperature compression seuil incendie")
    assert len(vector) == 128
    norm = sum(v * v for v in vector) ** 0.5
    assert abs(norm - 1.0) < 1e-6


def test_similar_texts_are_closer():
    embedder = HashingEmbedder()
    a = embedder.embed("temperature seuil alarme compresseur")
    b = embedder.embed("température seuil alarme compresseur")
    c = embedder.embed("pompe centrifuge débit hydraulique")
    sim_ab = _cosine(a, b)
    sim_ac = _cosine(a, c)
    assert sim_ab > sim_ac


def test_embedding_is_deterministic():
    embedder = HashingEmbedder()
    assert embedder.embed("four électrique thermique") == embedder.embed("four électrique thermique")


def _cosine(a, b):
    return sum(x * y for x, y in zip(a, b, strict=False))
