# Complete Startup Guide - Correct Order

Follow these steps **IN EXACT ORDER** to get auto-registration working.

---

## Prerequisites Check

Before starting, verify:
```bash
# Check if Consul is running
pgrep -f "consul agent"

# If NOT running, start it:
consul agent -dev &
```

---

## Step 1: Start SSH Tunnel with Port Forwarding

**CRITICAL:** Start this FIRST, before starting any Spring Boot instances!

```bash
ssh -p 2222 s367911@helios.se.ifmo.ru \
  -R 8500:localhost:8500 \
  -L 58123:localhost:58123 \
  -L 58124:localhost:58124 \
  -L 58122:localhost:58122 \
  -o ServerAliveInterval=60 \
  -o ServerAliveCountMax=3 \
  -N
```

**What each port does:**
- `-R 8500:localhost:8500` - **REVERSE tunnel**: Helios can reach your local Consul (for service registration)
- `-L 58123:localhost:58123` - **FORWARD tunnel**: Your local Consul can reach instance 1 (for health checks)
- `-L 58124:localhost:58124` - **FORWARD tunnel**: Your local Consul can reach instance 2 (for health checks)
- `-L 58122:localhost:58122` - **FORWARD tunnel**: Your local Consul can reach instance 3 (for health checks)

**This tunnel must stay running!** Open a new terminal for next steps.

---

## Step 2: Verify Tunnel is Working

In a NEW terminal window:

```bash
# Test reverse tunnel (can helios reach your Consul?)
ssh -p 2222 s367911@helios.se.ifmo.ru "curl -s http://localhost:8500/v1/status/leader"
# Should return: "127.0.0.1:8300"

# Test forward tunnel (can you reach helios?)
curl -k https://localhost:58123/actuator/health
# Should return: connection refused (instance not running yet - that's OK!)
```

---

## Step 3: Start Spring Boot Instances on Helios

**NOW** start the instances (tunnel is ready to handle registration):

### Terminal 1 - Start Instance 1:
```bash
ssh -p 2222 s367911@helios.se.ifmo.ru "cd ~ && nohup java -Xmx512m -Xms256m -jar person-service-0.0.1-SNAPSHOT.jar \
  --spring.config.additional-location=application-helios.properties \
  --spring.profiles.active=helios \
  > instance1.log 2>&1 & echo \$!"
```

### Terminal 2 - Start Instance 2:
```bash
ssh -p 2222 s367911@helios.se.ifmo.ru "cd ~ && nohup java -Xmx512m -Xms256m -jar person-service-0.0.1-SNAPSHOT.jar \
  --spring.config.additional-location=application-helios-instance2.properties \
  --spring.profiles.active=helios \
  > instance2.log 2>&1 & echo \$!"
```

**Wait 30-60 seconds** for Spring Boot to start and register.

---

## Step 4: Verify Auto-Registration Worked

```bash
# Watch for instances to appear (auto-updates every 2 seconds)
./watch-registration.sh

# Or manually check:
curl -s http://localhost:8500/v1/catalog/service/person-service | python3 -m json.tool

# Check health status:
curl -s http://localhost:8500/v1/health/service/person-service | python3 -m json.tool
```

**Expected output:** 2 instances registered with "passing" health checks.

---

## Step 5: Start consul-template

consul-template watches Consul and auto-generates HAProxy config:

```bash
consul-template -config=consul-template.hcl &
```

**Wait 10-20 seconds** for initial config generation.

---

## Step 6: Start HAProxy

```bash
haproxy -f haproxy.cfg &
```

---

## Step 7: Verify Everything is Working

### Check HAProxy Config:
```bash
grep "server MacBook" haproxy.cfg
# Should show 2 instances
```

### Check HAProxy Stats:
```bash
open http://localhost:8404/stats
# Look at "person_service_backend" - should show 2 servers UP
```

### Test Load Balancing:
```bash
for i in {1..10}; do
  curl -k -s https://localhost:8080/persons/count
  echo ""
done
```

### Check Traffic Distribution:
```bash
curl -s "http://localhost:8404/stats;csv" | grep "person_service_backend,MacBook" | \
  awk -F',' '{print "Server: " $2 " - Bytes In: " $9 " - Bytes Out: " $10}'
```

Both instances should show traffic!

---

## Complete Startup Script

You can also use this one-command startup (after tunnel is running):

```bash
# 1. Start instances on helios
ssh -p 2222 s367911@helios.se.ifmo.ru "cd ~ && \
  nohup java -Xmx512m -Xms256m -jar person-service-0.0.1-SNAPSHOT.jar \
    --spring.config.additional-location=application-helios.properties \
    --spring.profiles.active=helios > instance1.log 2>&1 & \
  sleep 3 && \
  nohup java -Xmx512m -Xms256m -jar person-service-0.0.1-SNAPSHOT.jar \
    --spring.config.additional-location=application-helios-instance2.properties \
    --spring.profiles.active=helios > instance2.log 2>&1 &"

# 2. Wait for registration (locally)
sleep 60

# 3. Start consul-template and HAProxy (locally)
consul-template -config=consul-template.hcl &
sleep 15
haproxy -f haproxy.cfg &
```

---

## Shutdown Script

To cleanly stop everything:

```bash
# Kill local processes
pkill -f consul-template
pkill -f haproxy
pkill -f "ssh.*helios"

# Kill remote instances
ssh -p 2222 s367911@helios.se.ifmo.ru "pkill -f person-service"
```

---

## Troubleshooting

### Problem: Instances don't appear in Consul

**Check 1:** Is tunnel running with `-R 8500`?
```bash
ps aux | grep "ssh.*helios.*8500"
```

**Check 2:** Can helios reach Consul through tunnel?
```bash
ssh -p 2222 s367911@helios.se.ifmo.ru "curl -s http://localhost:8500/v1/status/leader"
# Must return: "127.0.0.1:8300"
```

**Check 3:** Look at Spring Boot logs on helios:
```bash
ssh -p 2222 s367911@helios.se.ifmo.ru "tail -100 ~/instance1.log | grep -i consul"
```

**Fix:** Restart tunnel with `-R 8500`, then restart Spring instances.

---

### Problem: Health checks failing

**Check:** Can local Consul reach instances through forward tunnels?
```bash
curl -k https://localhost:58123/actuator/health
curl -k https://localhost:58124/actuator/health
```

**Fix:** Ensure forward tunnels (`-L 58123`, `-L 58124`) are in SSH command.

---

### Problem: HAProxy shows 0 traffic

**Check:** Is HAProxy running and using the right config?
```bash
pgrep -f haproxy
curl -s http://localhost:8404/stats | grep "person_service_backend"
```

**Fix:** Reload HAProxy config:
```bash
pkill -HUP haproxy
```

---

## For Lab Defense Demo

### Show Auto-Discovery Working:

1. **Show current state:**
   ```bash
   curl http://localhost:8500/v1/catalog/service/person-service | python3 -m json.tool
   open http://localhost:8404/stats
   ```

2. **Add 3rd instance (on helios):**
   ```bash
   ssh -p 2222 s367911@helios.se.ifmo.ru "cd ~ && \
     nohup java -Xmx512m -Xms256m -jar person-service-0.0.1-SNAPSHOT.jar \
       --spring.config.additional-location=application-helios-instance3.properties \
       --spring.profiles.active=helios > instance3.log 2>&1 &"
   ```

3. **Watch auto-discovery happen:**
   ```bash
   # Watch Consul
   watch -n 2 'curl -s http://localhost:8500/v1/catalog/service/person-service | python3 -c "import sys, json; print(len(json.load(sys.stdin)))"'

   # After ~60 seconds, check HAProxy
   grep "server MacBook" haproxy.cfg
   # Now shows 3 instances!
   ```

**Key message:** "When we added the 3rd instance, it automatically registered with Consul. consul-template detected this change within 60 seconds and regenerated HAProxy's configuration without any manual intervention. This is true dynamic service discovery!"

---

## Critical Success Factors

1. **SSH tunnel MUST be started BEFORE Spring instances**
2. **SSH tunnel MUST include `-R 8500:localhost:8500`** (reverse tunnel for registration)
3. **SSH tunnel MUST include `-L 58123/58124/58122`** (forward tunnels for health checks)
4. **Spring instances need 30-60 seconds to start and register**
5. **consul-template needs 10-20 seconds to detect changes and regenerate config**

Total time from instance start to HAProxy update: **60-90 seconds**
