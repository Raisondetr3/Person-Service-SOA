#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "======================================"
echo "Debugging Spring Boot Auto-Registration"
echo "======================================"

# Step 1: Check local Consul
echo -e "\n${YELLOW}[1] Checking Local Consul...${NC}"
if curl -s http://localhost:8500/v1/status/leader > /dev/null 2>&1; then
    LEADER=$(curl -s http://localhost:8500/v1/status/leader)
    echo -e "${GREEN}✓ Consul running - Leader: $LEADER${NC}"
else
    echo -e "${RED}✗ Consul NOT accessible on localhost:8500${NC}"
    echo "  Start with: consul agent -dev &"
    exit 1
fi

# Step 2: Test SSH connectivity
echo -e "\n${YELLOW}[2] Testing SSH Connection to Helios...${NC}"
if ssh -p 2222 -o ConnectTimeout=5 s367911@helios.se.ifmo.ru "echo OK" 2>/dev/null | grep -q "OK"; then
    echo -e "${GREEN}✓ SSH connection works${NC}"
else
    echo -e "${RED}✗ Cannot connect to helios${NC}"
    echo "  Check your SSH credentials"
    exit 1
fi

# Step 3: Test reverse tunnel
echo -e "\n${YELLOW}[3] Testing Reverse Tunnel (CRITICAL)...${NC}"
echo "   Checking if helios can reach local Consul through reverse tunnel..."

# This is the key test - can helios reach our local Consul?
RESULT=$(ssh -p 2222 s367911@helios.se.ifmo.ru "curl -s -m 3 http://localhost:8500/v1/status/leader 2>&1")

if echo "$RESULT" | grep -q "127.0.0.1:8300"; then
    echo -e "${GREEN}✓ Reverse tunnel IS WORKING!${NC}"
    echo "   Helios can reach local Consul at localhost:8500"
else
    echo -e "${RED}✗ Reverse tunnel NOT WORKING!${NC}"
    echo "   Helios cannot reach local Consul"
    echo ""
    echo "   Response from helios: $RESULT"
    echo ""
    echo "   ${YELLOW}This is the problem!${NC}"
    echo ""
    echo "   Fix: Start SSH tunnel with reverse port forwarding:"
    echo "   ${GREEN}ssh -p 2222 s367911@helios.se.ifmo.ru \\${NC}"
    echo "   ${GREEN}  -R 8500:localhost:8500 \\${NC}"
    echo "   ${GREEN}  -L 58123:localhost:58123 \\${NC}"
    echo "   ${GREEN}  -L 58124:localhost:58124 \\${NC}"
    echo "   ${GREEN}  -N${NC}"
    echo ""
    exit 1
fi

# Step 4: Check if Spring Boot instances are running on helios
echo -e "\n${YELLOW}[4] Checking Spring Boot Instances on Helios...${NC}"
JAVA_PROCS=$(ssh -p 2222 s367911@helios.se.ifmo.ru "pgrep -f person-service | wc -l" 2>/dev/null)

if [ "$JAVA_PROCS" -gt 0 ]; then
    echo -e "${GREEN}✓ Found $JAVA_PROCS Spring Boot instance(s) running${NC}"
    
    # Check the logs for registration attempts
    echo "   Checking if instances tried to register..."
    ssh -p 2222 s367911@helios.se.ifmo.ru "pgrep -f person-service" | while read PID; do
        echo "   Process $PID"
    done
else
    echo -e "${YELLOW}⚠ No Spring Boot instances running on helios${NC}"
    echo "   You need to start them for auto-registration to work"
fi

# Step 5: Check current Consul registration
echo -e "\n${YELLOW}[5] Current Services in Consul...${NC}"
SERVICES=$(curl -s http://localhost:8500/v1/catalog/service/person-service)
COUNT=$(echo "$SERVICES" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null)

if [ "$COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓ Found $COUNT registered instance(s)${NC}"
    echo "$SERVICES" | python3 -c "import sys, json; [print(f'  - {s[\"ServiceID\"]} on port {s[\"ServicePort\"]}') for s in json.load(sys.stdin)]"
else
    echo -e "${YELLOW}⚠ No instances registered yet${NC}"
fi

echo -e "\n======================================"
echo "Diagnosis Summary"
echo "======================================"

echo -e "\n${GREEN}If reverse tunnel is working:${NC}"
echo "  1. Restart Spring Boot instances on helios"
echo "  2. They should auto-register within ~30 seconds"
echo "  3. Watch: watch -n 2 'curl -s http://localhost:8500/v1/catalog/service/person-service'"
echo ""
echo -e "${YELLOW}If reverse tunnel is NOT working:${NC}"
echo "  1. Kill any existing SSH connections to helios"
echo "  2. Start new tunnel with -R flag (see command above)"
echo "  3. Wait 5 seconds, then restart Spring Boot instances"
echo ""
