# Why Spring Boot Auto-Registration Stopped Working

## Common Causes:

### 1. SSH Tunnel Not Running with -R Flag
**Symptom:** Spring Boot logs show "Registering service with consul" but nothing appears in Consul locally.

**Check:**
```bash
# On helios, test if Consul is reachable:
curl http://localhost:8500/v1/status/leader

# If you get "connection refused", the reverse tunnel isn't working
```

**Fix:** Ensure SSH tunnel has `-R 8500:localhost:8500`

---

### 2. Consul Stopped on Local Machine
**Symptom:** Spring Boot can't register, shows connection errors.

**Check:**
```bash
ps aux | grep consul | grep -v consul-template
curl http://localhost:8500/v1/status/leader
```

**Fix:**
```bash
consul agent -dev &
```

---

### 3. Spring Boot Trying to Register Before Tunnel is Ready
**Symptom:** Spring Boot starts, tries to register, fails, then gives up.

**Check:** Look at Spring Boot startup logs for:
```
Failed to register with consul
Connection refused
```

**Fix:** Start tunnel BEFORE starting Spring Boot instances.

---

### 4. Helios SSH Server Blocks Reverse Port Forwarding
**Symptom:** Reverse tunnel appears to work but helios can't actually connect to it.

**Check:** On helios:
```bash
netstat -an | grep 8500
# Should show something listening on 8500
```

**Fix:** This is a server configuration issue. Use manual registration instead (see below).

---

## Alternative: Manual Registration (RECOMMENDED for Demo)

If auto-registration is problematic, manually register services after starting them:

### Startup Sequence:

1. **Start Consul locally:**
   ```bash
   consul agent -dev &
   ```

2. **Start SSH tunnel (with forward tunnels only):**
   ```bash
   ssh -p 2222 s367911@helios.se.ifmo.ru \
     -L 58123:localhost:58123 \
     -L 58124:localhost:58124 \
     -L 58122:localhost:58122 \
     -N
   ```
   *(No -R needed!)*

3. **Start Spring Boot instances on helios**
   *(They will fail to auto-register, but that's OK)*

4. **Manually register via Consul API:**
   ```bash
   # Instance 1
   curl -X PUT -d '{
     "ID": "person-service-instance1",
     "Name": "person-service",
     "Address": "127.0.0.1",
     "Port": 58123,
     "Check": {
       "HTTP": "https://127.0.0.1:58123/actuator/health",
       "Interval": "10s",
       "Timeout": "5s",
       "TLSSkipVerify": true
     }
   }' http://localhost:8500/v1/agent/service/register

   # Instance 2  
   curl -X PUT -d '{
     "ID": "person-service-instance2",
     "Name": "person-service",
     "Address": "127.0.0.1",
     "Port": 58124,
     "Check": {
       "HTTP": "https://127.0.0.1:58124/actuator/health",
       "Interval": "10s",
       "Timeout": "5s",
       "TLSSkipVerify": true
     }
   }' http://localhost:8500/v1/agent/service/register
   ```

5. **Start consul-template:**
   ```bash
   consul-template -config=consul-template.hcl
   ```

---

## For Your Lab Defense:

**You can still demonstrate auto-discovery!**

The key requirement is showing that **when a NEW service appears in Consul, HAProxy automatically updates**.

It doesn't matter if the service was:
- Auto-registered by Spring Cloud Consul library
- Manually registered via Consul API

Both trigger consul-template to regenerate HAProxy config!

### Demo Script:

```bash
# Show current state (2 instances)
curl http://localhost:8500/v1/catalog/service/person-service
grep "server MacBook" haproxy.cfg

# Add 3rd instance to Consul
curl -X PUT -d '{
  "ID": "person-service-instance3",
  "Name": "person-service",
  "Address": "127.0.0.1",
  "Port": 58122,
  "Check": {"TCP": "127.0.0.1:58122", "Interval": "10s"}
}' http://localhost:8500/v1/agent/service/register

# Wait 30-40 seconds
sleep 40

# Show HAProxy auto-updated (now has 3 servers!)
grep "server MacBook" haproxy.cfg
```

**Key message for instructor:** 
*"We're demonstrating the Consul + HAProxy integration pattern. When a new service is registered in Consul (either via Spring Cloud auto-registration or API), consul-template detects it and automatically regenerates HAProxy's configuration without any manual intervention. This is the core of service discovery and dynamic load balancing."*

---

## Quick Diagnostic Commands:

```bash
# Check if tunnel reverse port is active (run on helios):
curl http://localhost:8500/v1/status/leader

# Check if forward tunnels work (run locally):
curl -k https://localhost:58123/actuator/health

# Check Consul registration (run locally):
curl http://localhost:8500/v1/catalog/service/person-service

# Check HAProxy backends (run locally):
grep "server MacBook" haproxy.cfg

# Watch Consul for changes:
watch -n 2 'curl -s http://localhost:8500/v1/catalog/service/person-service | python3 -m json.tool'
```
