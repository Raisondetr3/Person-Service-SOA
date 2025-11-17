#!/bin/bash

echo "======================================"
echo "Testing Reverse Tunnel to Consul"
echo "======================================"

echo ""
echo "1. Checking local Consul status..."
if curl -s http://localhost:8500/v1/status/leader > /dev/null 2>&1; then
    echo "   ✓ Consul is running locally on port 8500"
else
    echo "   ✗ Consul is NOT accessible on localhost:8500"
    exit 1
fi

echo ""
echo "2. Testing if SSH reverse tunnel would work..."
echo "   This requires the SSH tunnel to be running with:"
echo "   ssh -p 2222 s367911@helios.se.ifmo.ru -R 8500:localhost:8500 -N"
echo ""

# Check if helios SSH server allows GatewayPorts
echo "3. Checking SSH server configuration on helios..."
echo "   (This will attempt to connect and check)"
echo ""

ssh -p 2222 s367911@helios.se.ifmo.ru "netstat -an | grep 8500 | head -3" 2>/dev/null

if [ $? -eq 0 ]; then
    echo ""
    echo "   If you see a line with ':8500' above, the reverse tunnel is active!"
    echo "   If not, the tunnel may not be running or may be blocked."
else
    echo "   Could not check (may need to start the tunnel first)"
fi

echo ""
echo "======================================"
echo "To test Spring Boot auto-registration:"
echo "======================================"
echo ""
echo "1. Start SSH tunnel in a terminal:"
echo "   ssh -p 2222 s367911@helios.se.ifmo.ru \\"
echo "     -R 8500:localhost:8500 \\"
echo "     -L 58123:localhost:58123 \\"
echo "     -L 58124:localhost:58124 \\"
echo "     -L 58122:localhost:58122 \\"
echo "     -N"
echo ""
echo "2. On helios, check if tunnel is working:"
echo "   curl http://localhost:8500/v1/status/leader"
echo "   (Should return: \"127.0.0.1:8300\")"
echo ""
echo "3. Restart Spring Boot instance on helios:"
echo "   java -jar person-service-0.0.1-SNAPSHOT.jar \\"
echo "     --spring.config.additional-location=application-helios.properties \\"
echo "     --spring.profiles.active=helios"
echo ""
echo "4. Watch Consul locally for registration:"
echo "   watch -n 2 'curl -s http://localhost:8500/v1/catalog/service/person-service'"
echo ""
