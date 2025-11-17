#!/bin/bash

echo "=========================================="
echo "Starting Complete Stack"
echo "=========================================="

# Step 1: Check Consul
echo ""
echo "[1/6] Checking Consul..."
if pgrep -f "consul agent" > /dev/null; then
    echo "✓ Consul already running"
else
    echo "✗ Consul not running. Starting..."
    consul agent -dev &
    sleep 5
fi

# Step 2: Start SSH Tunnel
echo ""
echo "[2/6] Starting SSH Tunnel with Port Forwarding..."
echo "This will run in background. Press Ctrl+C if it hangs, then run manually:"
echo "ssh -p 2222 s367911@helios.se.ifmo.ru -R 8500:localhost:8500 -L 58123:localhost:58123 -L 58124:localhost:58124 -L 58122:localhost:58122 -N &"

ssh -p 2222 s367911@helios.se.ifmo.ru \
  -R 8500:localhost:8500 \
  -L 58123:localhost:58123 \
  -L 58124:localhost:58124 \
  -L 58122:localhost:58122 \
  -o ServerAliveInterval=60 \
  -o ServerAliveCountMax=3 \
  -N &

TUNNEL_PID=$!
echo "✓ SSH Tunnel started (PID: $TUNNEL_PID)"
sleep 5

# Step 3: Verify Tunnel
echo ""
echo "[3/6] Verifying SSH Tunnel..."
CONSUL_CHECK=$(ssh -p 2222 s367911@helios.se.ifmo.ru "curl -s -m 3 http://localhost:8500/v1/status/leader" 2>/dev/null)
if echo "$CONSUL_CHECK" | grep -q "127.0.0.1:8300"; then
    echo "✓ Reverse tunnel working! Helios can reach Consul"
else
    echo "✗ Reverse tunnel NOT working!"
    echo "Response: $CONSUL_CHECK"
    exit 1
fi

# Step 4: Start Spring Boot Instances
echo ""
echo "[4/6] Starting Spring Boot instances on Helios..."
ssh -p 2222 s367911@helios.se.ifmo.ru "cd ~ && \
  nohup java -Xmx512m -Xms256m -jar person-service-0.0.1-SNAPSHOT.jar \
    --spring.config.additional-location=application-helios.properties \
    --spring.profiles.active=helios > instance1.log 2>&1 & \
  sleep 3 && \
  nohup java -Xmx512m -Xms256m -jar person-service-0.0.1-SNAPSHOT.jar \
    --spring.config.additional-location=application-helios-instance2.properties \
    --spring.profiles.active=helios > instance2.log 2>&1 &"

echo "✓ Spring Boot instances starting..."
echo "  Waiting 60 seconds for startup and auto-registration..."

# Wait and watch for registration
for i in {1..30}; do
    sleep 2
    COUNT=$(curl -s http://localhost:8500/v1/catalog/service/person-service | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
    if [ "$COUNT" -ge 2 ]; then
        echo "✓ Both instances auto-registered in Consul!"
        break
    fi
    echo -n "."
done
echo ""

# Step 5: Start consul-template
echo ""
echo "[5/6] Starting consul-template..."
consul-template -config=consul-template.hcl &
echo "✓ consul-template started"
echo "  Waiting 15 seconds for initial config generation..."
sleep 15

# Step 6: Start HAProxy
echo ""
echo "[6/6] Starting HAProxy..."
haproxy -f haproxy.cfg &
echo "✓ HAProxy started"

# Final verification
echo ""
echo "=========================================="
echo "Startup Complete!"
echo "=========================================="
echo ""
echo "Verification:"
echo ""
curl -s http://localhost:8500/v1/catalog/service/person-service | python3 -c "import sys, json; d=json.load(sys.stdin); print(f'Consul: {len(d)} instances registered')"
echo ""
grep "server MacBook" haproxy.cfg | wc -l | xargs echo "HAProxy: detected"
echo ""
echo "Next steps:"
echo "  1. Open http://localhost:8404/stats (HAProxy stats)"
echo "  2. Test: curl -k https://localhost:8080/persons/count"
echo "  3. Test: open http://localhost:8500/ui (Consul UI)"
