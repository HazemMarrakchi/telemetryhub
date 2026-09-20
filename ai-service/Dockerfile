FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY knowledge ./knowledge

ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app

EXPOSE 8090
ENTRYPOINT ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]