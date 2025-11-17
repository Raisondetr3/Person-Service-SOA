#!/bin/bash

echo "=========================================="
echo "Shutting Down Complete Stack"
echo "=========================================="

echo ""
echo "[1/4] Stopping HAProxy..."
pkill -f haproxy && echo "✓ HAProxy stopped" || echo "✓ HAProxy not running"

echo ""
echo "[2/4] Stopping consul-template..."
pkill -f consul-template && echo "✓ consul-template stopped" || echo "✓ consul-template not running"

echo ""
echo "[3/4] Stopping Spring Boot instances on Helios..."
ssh -p 2222 s367911@helios.se.ifmo.ru "pkill -f person-service" && echo "✓ Remote instances stopped" || echo "✓ No remote instances running"

echo ""
echo "[4/4] Stopping SSH tunnels..."
pkill -f "ssh.*helios" && echo "✓ SSH tunnels stopped" || echo "✓ No SSH tunnels running"

echo ""
echo "=========================================="
echo "Shutdown Complete!"
echo "=========================================="
echo ""
echo "Note: Consul is still running. To stop it:"
echo "  pkill -f 'consul agent'"
