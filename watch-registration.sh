#!/bin/bash

echo "Watching Consul for auto-registration..."
echo "Press Ctrl+C to stop"
echo ""

for i in {1..60}; do
    clear
    echo "========================================"
    echo "Watching Consul - Attempt $i/60"
    echo "========================================"
    echo ""
    
    RESULT=$(curl -s http://localhost:8500/v1/catalog/service/person-service)
    COUNT=$(echo "$RESULT" | python3 -c "import sys, json; d=json.load(sys.stdin); print(len(d)); [print(f'  ✓ {s[\"ServiceID\"]} on port {s[\"ServicePort\"]}') for s in d]" 2>/dev/null || echo "0")
    
    if [ "$COUNT" != "0" ]; then
        echo ""
        echo "✅ SUCCESS! Auto-registration detected!"
        echo ""
        exit 0
    else
        echo "⏳ Waiting for instances to register..."
        echo "   (Spring Boot takes ~30-60s to start)"
    fi
    
    sleep 2
done

echo ""
echo "⚠️ Timeout - instances did not auto-register"
echo "Check Spring Boot logs on helios for errors"
