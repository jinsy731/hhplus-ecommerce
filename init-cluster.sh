#!/bin/bash
set -e

echo "Wait for Redis nodes to be ready..."
sleep 5

echo "Creating Redis cluster..."
yes yes | redis-cli --cluster create \
  redis-node1:7001 \
  redis-node2:7002 \
  redis-node3:7003 \
  --cluster-replicas 0

echo "Redis Cluster created successfully."

