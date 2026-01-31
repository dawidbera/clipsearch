#!/bin/bash
set -e

NAMESPACE="dawidbera-dev"

echo "🚀 Starting Native OpenShift Build & Deploy..."

# 1. API Build
echo "📦 Building API (Backend)..."
cp backend/api/Dockerfile backend/Dockerfile
oc start-build clipsearch-api --from-dir=./backend --follow -n $NAMESPACE
rm backend/Dockerfile

# 2. Worker Build
echo "📦 Building Worker (Backend)..."
cp backend/worker/Dockerfile backend/Dockerfile
oc start-build clipsearch-worker --from-dir=./backend --follow -n $NAMESPACE
rm backend/Dockerfile

# 3. Frontend Build
echo "📦 Building Frontend..."
oc start-build clipsearch-frontend --from-dir=./frontend --follow -n $NAMESPACE

echo "📦 Applying K8s manifests..."
oc apply -k deploy/overlays/openshift-sandbox -n $NAMESPACE

echo "🔄 Waiting for deployments to update..."
oc rollout status deployment/clipsearch-api -n $NAMESPACE
oc rollout status deployment/clipsearch-worker -n $NAMESPACE
oc rollout status deployment/clipsearch-frontend -n $NAMESPACE

echo "✅ Deployment complete!"
