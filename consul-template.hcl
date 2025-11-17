consul {
  address = "localhost:8500"
  retry {
    enabled  = true
    attempts = 12
    backoff  = "250ms"
  }
}

wait {
  min = "30s"
  max = "60s"
}

deduplicate {
  enabled = true
  prefix = "consul-template/dedup"
}

template {
  source      = "haproxy.cfg.tmpl"
  destination = "haproxy.cfg"
  command     = "haproxy -D -f haproxy.cfg -sf $(pgrep haproxy) || haproxy -D -f haproxy.cfg"
  command_timeout = "10s"

  backup = true

  wait {
    min = "15s"
    max = "30s"
  }

  perms = "0644"

  error_on_missing_key = false
}

log_level = "info"
