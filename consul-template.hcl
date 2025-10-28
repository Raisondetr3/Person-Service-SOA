consul {
  address = "localhost:8500"
  retry {
    enabled  = true
    attempts = 12
    backoff  = "250ms"
  }
}

template {
  source      = "haproxy.cfg.tmpl"
  destination = "haproxy.cfg"
  command     = "pkill -HUP haproxy || haproxy -f haproxy.cfg"
  command_timeout = "30s"
}

# Log level
log_level = "info"
