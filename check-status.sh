#!/bin/bash
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "======================================"
echo "Lab Setup Status Check"
echo "======================================"

# Consul
echo -e "\n${YELLOW}[1] Consul${NC}"
if pgrep -x "consul" > /dev/null; then
    echo -e "${GREEN}✓ Running${NC}"
    COUNT=$(curl -s http://localhost:8500/v1/catalog/service/person-service | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null)
    echo -e "${GREEN}✓ ${COUNT} person-service instance(s) registered${NC}"
else
    echo -e "${RED}✗ Not running${NC}"
fi

# SSH Tunnel
echo -e "\n${YELLOW}[2] SSH Tunnel${NC}"
if curl -k -s -m 2 https://localhost:58123/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Port 58123 accessible${NC}"
else
    echo -e "${RED}✗ Port 58123 NOT accessible - start SSH tunnel!${NC}"
    echo "  Run: ssh -p 2222 s367911@helios.se.ifmo.ru -L 58123:localhost:58123 -L 58124:localhost:58124 -N"
fi

# consul-template
echo -e "\n${YELLOW}[3] consul-template${NC}"
if pgrep -x "consul-template" > /dev/null; then
    echo -e "${GREEN}✓ Running${NC}"
else
    echo -e "${RED}✗ Not running${NC}"
    echo "  Run: consul-template -config=consul-template.hcl"
fi

# HAProxy
echo -e "\n${YELLOW}[4] HAProxy${NC}"
if pgrep -x "haproxy" > /dev/null; then
    echo -e "${GREEN}✓ Running${NC}"
    BACKEND_COUNT=$(grep -c "server MacBook" haproxy.cfg 2>/dev/null || echo 0)
    if [ "$BACKEND_COUNT" -ge 2 ]; then
        echo -e "${GREEN}✓ ${BACKEND_COUNT} Spring backend(s) configured${NC}"
    else
        echo -e "${RED}✗ No Spring backends in config - consul-template may not be running${NC}"
    fi
else
    echo -e "${RED}✗ Not running${NC}"
fi

# Load Balancing Test
echo -e "\n${YELLOW}[5] Load Balancing Test${NC}"
if curl -k -s -m 3 https://localhost:8080/persons/count > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Port 8080 responding${NC}"
else
    echo -e "${RED}✗ Port 8080 not responding${NC}"
fi

echo -e "\n======================================"
echo "Summary"
echo "======================================"
echo "If all checks show ✓, you're ready for demo!"
echo ""
echo "Useful URLs:"
echo "  Consul UI:     http://localhost:8500/ui"
echo "  HAProxy stats: http://localhost:8404/stats"
