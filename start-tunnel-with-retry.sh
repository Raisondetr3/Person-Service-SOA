#!/bin/bash

echo "Starting SSH tunnel with all port forwards..."
echo ""
echo "This will create:"
echo "  -R 8500  : helios:8500 → local:8500 (Consul access)"
echo "  -L 58123 : local:58123 → helios:58123 (Spring instance 1)"
echo "  -L 58124 : local:58124 → helios:58124 (Spring instance 2)"  
echo "  -L 58122 : local:58122 → helios:58122 (Spring instance 3)"
echo ""
echo "Press Ctrl+C to stop the tunnel"
echo ""

# Kill any existing SSH sessions to helios
pkill -f "ssh.*helios.*8500"

sleep 2

# Start tunnel with verbose output for debugging
ssh -v -p 2222 s367911@helios.se.ifmo.ru \
  -R 8500:localhost:8500 \
  -L 58123:localhost:58123 \
  -L 58124:localhost:58124 \
  -L 58122:localhost:58122 \
  -o ServerAliveInterval=60 \
  -o ServerAliveCountMax=3 \
  -N
