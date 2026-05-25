FROM python:3.11-slim

WORKDIR /app

# Dépendances système pour implicit (BLAS)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libopenblas-dev \
    && rm -rf /var/lib/apt/lists/*

# --- Étape 1 : dépendances Python ---
COPY requirements.txt .
RUN pip install --no-cache-dir \
    --index-url https://pypi.org/simple/ \
    --timeout 300 \
    --retries 10 \
    -r requirements.txt

# --- Étape 2 : pré-télécharger le modèle ONNX ---
# fastembed télécharge le modèle au 1er appel ; on le fait ici pour que le
# container démarre instantanément sans attendre le téléchargement.
RUN python -c "from fastembed import TextEmbedding; TextEmbedding('sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2')"

COPY . .

EXPOSE 8090

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8090"]
