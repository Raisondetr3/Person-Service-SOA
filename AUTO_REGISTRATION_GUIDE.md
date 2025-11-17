# Complete Auto-Registration Setup Guide

## Current Status ✅

Your environment is READY for auto-registration:
- ✅ Consul running locally
- ✅ SSH reverse tunnel working (helios can reach Consul)
- ✅ Consul cleared and ready

## Step-by-Step Instructions

### **ON HELIOS (SSH to helios):**

1. **Go to your project directory:**
   ```bash
   cd ~
   ```

2. **Start Instance 1:**
   ```bash
   nohup java -jar person-service-0.0.1-SNAPSHOT.jar \
     --spring.config.additional-location=application-helios.properties \
     --spring.profiles.active=helios \
     > instance1.log 2>&1 &
   ```

3. **Start Instance 2:**
   ```bash
   nohup java -jar person-service-0.0.1-SNAPSHOT.jar \
     --spring.config.additional-location=application-helios-instance2.properties \
     --spring.profiles.active=helios \
     > instance2.log 2>&1 &
   ```

4. **Check instances are starting:**
   ```bash
   tail -f instance1.log
   # Look for: "Registering service with consul"
   # Press Ctrl+C to exit
   ```

### **ON YOUR LOCAL MACHINE:**

1. **Watch for auto-registration:**
   ```bash
   ./watch-registration.sh
   ```

   You should see instances appear within ~30-60 seconds!

2. **Or manually check:**
   ```bash
   curl -s http://localhost:8500/v1/catalog/service/person-service | python3 -m json.tool
   ```

3. **Once registered, verify health:**
   ```bash
   curl -s http://localhost:8500/v1/health/service/person-service?passing | python3 -m json.tool
   ```

---

## What to Expect

### Timeline:
- **0-30s**: Spring Boot instances start
- **30-45s**: Instances connect to Consul (via reverse tunnel)
- **45-60s**: Health checks pass, instances marked as "passing"
- **60-90s**: consul-template detects changes
- **90s+**: HAProxy config auto-updated!

### Success Indicators:

**In Spring Boot logs (on helios):**
```
Registering service with consul: NewService{id='person-service-instance1', name='person-service'...
```

**In local Consul:**
```bash
$ curl http://localhost:8500/v1/catalog/service/person-service
[
  {
    "ServiceID": "person-service-instance1",
    "ServiceName": "person-service",
    "ServicePort": 58123,
    ...
  },
  {
    "ServiceID": "person-service-instance2",
    "ServiceName": "person-service",
    "ServicePort": 58124,
    ...
  }
]
```

**In HAProxy config (after consul-template runs):**
```bash
$ grep "server MacBook" haproxy.cfg
server MacBook-Air-Raison.local-person-service-instance1 127.0.0.1:58123 ...
server MacBook-Air-Raison.local-person-service-instance2 127.0.0.1:58124 ...
```

---

## Troubleshooting

### If instances don't appear in Consul:

1. **Check Spring Boot logs on helios:**
   ```bash
   tail -100 instance1.log | grep -A5 consul
   ```

   Look for errors like:
   - "Connection refused" → Reverse tunnel not working
   - "Failed to register" → Check Consul is running locally

2. **Verify reverse tunnel from helios:**
   ```bash
   curl http://localhost:8500/v1/status/leader
   # Should return: "127.0.0.1:8300"
   ```

3. **Check instance properties:**
   ```bash
   cat application-helios.properties | grep consul
   ```

   Verify:
   - `spring.cloud.consul.host=localhost`
   - `spring.cloud.consul.port=8500`
   - `spring.cloud.consul.discovery.enabled=true`
   - `spring.cloud.consul.discovery.register=true`

### If health checks fail:

1. **Check forward tunnel works (locally):**
   ```bash
   curl -k https://localhost:58123/actuator/health
   # Should return: {"status":"UP",...}
   ```

2. **Check health check configuration in Consul:**
   ```bash
   curl -s http://localhost:8500/v1/health/checks/person-service | python3 -m json.tool
   ```

---

## Verification Commands

```bash
# Check Consul has services
curl http://localhost:8500/v1/catalog/service/person-service

# Check health status
curl http://localhost:8500/v1/health/service/person-service?passing

# Check HAProxy config updated
grep "server MacBook" haproxy.cfg

# Test load balancing
curl -k https://localhost:8080/persons/count

# View Consul UI
open http://localhost:8500/ui

# View HAProxy stats
open http://localhost:8404/stats
```

---

## For Your Demo

Once auto-registration is working, you can demonstrate:

### 1. Show Current State:
```bash
curl http://localhost:8500/v1/catalog/service/person-service
grep "server MacBook" haproxy.cfg
```

### 2. Add 3rd Instance:

**On helios:**
```bash
nohup java -jar person-service-0.0.1-SNAPSHOT.jar \
  --spring.config.additional-location=application-helios-instance3.properties \
  --spring.profiles.active=helios \
  > instance3.log 2>&1 &
```

**Locally, watch it auto-register:**
```bash
./watch-registration.sh
```

### 3. Show HAProxy Auto-Updated:
```bash
# Wait 60-90 seconds for consul-template
grep "server MacBook" haproxy.cfg  # Now shows 3 instances!
```

**Key message:** 
*"When we started the 3rd instance, it automatically registered with Consul. consul-template detected this change and regenerated HAProxy's configuration without any manual intervention. This is true dynamic service discovery and load balancing!"*
