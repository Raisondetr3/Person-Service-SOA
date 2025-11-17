#!/bin/bash

echo "Starting SSH tunnel to helios with port forwarding..."
echo ""
echo "This will create:"
echo "  - Reverse tunnel: helios:8500 → local:8500 (for Consul registration)"
echo "  - Forward tunnel: local:58123 → helios:58123 (Spring instance 1)"
echo "  - Forward tunnel: local:58124 → helios:58124 (Spring instance 2)"
echo ""

ssh -p 2222 s367911@helios.se.ifmo.ru \
  -R 8500:localhost:8500 \
  -L 58123:localhost:58123 \
  -L 58124:localhost:58124 \
  -N

