#!/bin/bash
# ClipSearch - OpenShift Deployment Script

set -e

echo "🚀 Starting ClipSearch deployment to OpenShift..."

# 1. Check if logged in to OpenShift
if ! oc whoami &> /dev/null; then
    echo "❌ Error: Not logged in to OpenShift. Please run 'oc login' first."
    exit 1
fi

# 2. Get current namespace from Kustomize
NAMESPACE=$(grep "namespace:" deploy/overlays/openshift-sandbox/kustomization.yaml | awk '{print $2}')
echo "🎯 Target namespace: $NAMESPACE"

# 2.5 Build and Push images
echo "🛠 Building and Pushing images to GHCR..."
# We assume user is logged in to docker/podman to ghcr.io
docker build -t ghcr.io/dawidbera/clipsearch-api:latest -f backend/api/Dockerfile backend
docker build -t ghcr.io/dawidbera/clipsearch-worker:latest -f backend/worker/Dockerfile backend
docker build -t ghcr.io/dawidbera/clipsearch-frontend:latest -f frontend/Dockerfile frontend

docker push ghcr.io/dawidbera/clipsearch-api:latest
docker push ghcr.io/dawidbera/clipsearch-worker:latest
docker push ghcr.io/dawidbera/clipsearch-frontend:latest

# 3. Apply the configuration
echo "📦 Applying K8s manifests via Kustomize..."
oc apply -k deploy/overlays/openshift-sandbox/

# 3.5 Force rollout to ensure latest image is pulled
echo "🔄 Triggering rollout to pull latest images..."
oc rollout restart deployment/clipsearch-api -n "$NAMESPACE"
oc rollout restart deployment/clipsearch-worker -n "$NAMESPACE"
oc rollout restart deployment/clipsearch-frontend -n "$NAMESPACE"

# 4. Wait for key components
echo "⏳ Waiting for Elasticsearch to be ready..."
oc wait --for=condition=available deployment/elasticsearch -n "$NAMESPACE" --timeout=300s

echo "⏳ Waiting for API to be ready..."
oc wait --for=condition=available deployment/clipsearch-api -n "$NAMESPACE" --timeout=300s

echo "✅ Deployment finished successfully!"
echo "🌐 You can find your routes with: oc get routes -n $NAMESPACE"
