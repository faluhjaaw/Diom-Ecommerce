#!/bin/bash
# GKE Deployment Script — Diom E-Commerce
# Usage: ./deploy.sh <PROJECT_ID> <GATEWAY_EXTERNAL_IP>
# Example: ./deploy.sh my-gcp-project 34.90.12.45

set -e

PROJECT_ID=${1:?Usage: ./deploy.sh <PROJECT_ID> <GATEWAY_EXTERNAL_IP>}
GATEWAY_IP=${2:?Usage: ./deploy.sh <PROJECT_ID> <GATEWAY_EXTERNAL_IP>}
REGION="us-central1"
REGISTRY="${REGION}-docker.pkg.dev/${PROJECT_ID}/diom-repo"
BACKEND_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND_DIR="$(cd "$(dirname "$0")/../../frontend" && pwd)"

echo "==> Project: $PROJECT_ID"
echo "==> Registry: $REGISTRY"
echo "==> Gateway IP: $GATEWAY_IP"

# ── 1. Configure Docker auth ──────────────────────────────────────────────────
echo "==> Configuring Docker for Artifact Registry..."
gcloud auth configure-docker ${REGION}-docker.pkg.dev --quiet

# ── 2. Build & Push backend services ─────────────────────────────────────────
SERVICES=(auth-service product-service customer-service gateway-service discovery-service avis-service message-service recommendation-service search-service)

for SERVICE in "${SERVICES[@]}"; do
  echo "==> Building $SERVICE..."
  docker build -t "${REGISTRY}/${SERVICE}:latest" "${BACKEND_DIR}/${SERVICE}"
  echo "==> Pushing $SERVICE..."
  docker push "${REGISTRY}/${SERVICE}:latest"
done

# ── 3. Build & Push frontend ──────────────────────────────────────────────────
echo "==> Building frontend with VITE_API_URL=http://${GATEWAY_IP}..."
docker build \
  --build-arg VITE_API_URL="http://${GATEWAY_IP}" \
  -t "${REGISTRY}/frontend:latest" \
  "${FRONTEND_DIR}"
docker push "${REGISTRY}/frontend:latest"

# ── 4. Replace PROJECT_ID in manifests ───────────────────────────────────────
echo "==> Patching manifests with PROJECT_ID=${PROJECT_ID}..."
K8S_DIR="$(dirname "$0")"
find "${K8S_DIR}" -name "*.yaml" -exec sed -i "s/PROJECT_ID/${PROJECT_ID}/g" {} \;

# ── 5. Deploy ─────────────────────────────────────────────────────────────────
echo "==> Creating namespace..."
kubectl apply -f "${K8S_DIR}/namespace.yaml"

echo "==> Applying secrets & config..."
kubectl apply -f "${K8S_DIR}/secrets.yaml"
kubectl apply -f "${K8S_DIR}/configmap.yaml"

echo "==> Deploying infrastructure..."
kubectl apply -f "${K8S_DIR}/infra/mongodb.yaml"
kubectl apply -f "${K8S_DIR}/infra/mysql.yaml"
kubectl apply -f "${K8S_DIR}/infra/redis.yaml"
kubectl apply -f "${K8S_DIR}/infra/zookeeper.yaml"

echo "==> Waiting for Zookeeper..."
kubectl rollout status statefulset/zookeeper -n diom --timeout=120s

kubectl apply -f "${K8S_DIR}/infra/kafka.yaml"

echo "==> Waiting for Kafka..."
kubectl rollout status statefulset/kafka -n diom --timeout=120s

kubectl apply -f "${K8S_DIR}/infra/qdrant.yaml"
kubectl apply -f "${K8S_DIR}/infra/ollama.yaml"

echo "==> Deploying discovery-service (Eureka)..."
kubectl apply -f "${K8S_DIR}/services/discovery-service.yaml"

echo "==> Waiting for Eureka..."
kubectl rollout status deployment/discovery-service -n diom --timeout=180s

echo "==> Deploying application services..."
kubectl apply -f "${K8S_DIR}/services/customer-service.yaml"
kubectl apply -f "${K8S_DIR}/services/auth-service.yaml"
kubectl apply -f "${K8S_DIR}/services/product-service.yaml"
kubectl apply -f "${K8S_DIR}/services/avis-service.yaml"
kubectl apply -f "${K8S_DIR}/services/message-service.yaml"
kubectl apply -f "${K8S_DIR}/services/search-service.yaml"
kubectl apply -f "${K8S_DIR}/services/recommendation-service.yaml"

echo "==> Waiting for core services..."
kubectl rollout status deployment/customer-service -n diom --timeout=180s
kubectl rollout status deployment/auth-service -n diom --timeout=180s

echo "==> Deploying gateway..."
kubectl apply -f "${K8S_DIR}/services/gateway-service.yaml"

echo "==> Deploying frontend..."
kubectl apply -f "${K8S_DIR}/frontend/frontend.yaml"

echo ""
echo "==> DONE. Check status:"
echo "    kubectl get pods -n diom"
echo "    kubectl get svc -n diom"
echo ""
echo "==> Get external IPs:"
echo "    kubectl get svc gateway-service frontend -n diom"
