package ru.itmo.person_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class ConsulAutoRegistration implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${spring.cloud.consul.discovery.instance-id}")
    private String instanceId;

    @Value("${server.port}")
    private Integer servicePort;

    @Value("${spring.cloud.consul.host:localhost}")
    private String consulHost;

    @Value("${spring.cloud.consul.port:8500}")
    private Integer consulPort;

    @Value("${spring.cloud.consul.discovery.hostname:localhost}")
    private String hostname;

    @Value("${spring.cloud.consul.discovery.scheme:https}")
    private String scheme;

    @Value("${spring.cloud.consul.discovery.health-check-path:/actuator/health}")
    private String healthCheckPath;

    @Value("${spring.cloud.consul.discovery.health-check-interval:10s}")
    private String healthCheckInterval;

    @Value("${spring.cloud.consul.discovery.health-check-timeout:5s}")
    private String healthCheckTimeout;

    @Value("${spring.cloud.consul.discovery.health-check-critical-timeout:30s}")
    private String healthCheckCriticalTimeout;

    @Value("${spring.cloud.consul.discovery.enabled:true}")
    private Boolean discoveryEnabled;

    @Value("${spring.cloud.consul.discovery.register:true}")
    private Boolean registerEnabled;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!discoveryEnabled || !registerEnabled) {
            log.info("Consul service registration is disabled");
            return;
        }

        log.info("==============================================");
        log.info("Starting explicit Consul service registration");
        log.info("==============================================");
        log.info("Service Name: {}", serviceName);
        log.info("Instance ID: {}", instanceId);
        log.info("Service Port: {}", servicePort);
        log.info("Consul URL: http://{}:{}", consulHost, consulPort);
        log.info("Hostname: {}", hostname);
        log.info("Health Check URL: {}://{}:{}{}", scheme, hostname, servicePort, healthCheckPath);

        try {
            registerWithConsul();
        } catch (Exception e) {
            log.error("Failed to register with Consul", e);
            log.error("Error details: {}", e.getMessage());

            // Log connection details for debugging
            log.error("Connection details:");
            log.error("  Consul endpoint: http://{}:{}/v1/agent/service/register", consulHost, consulPort);
            log.error("  Service: {}", serviceName);
            log.error("  Instance: {}", instanceId);
        }
    }

    private void registerWithConsul() {
        String consulUrl = String.format("http://%s:%d/v1/agent/service/register", consulHost, consulPort);

        Map<String, Object> registration = new HashMap<>();
        registration.put("ID", instanceId);
        registration.put("Name", serviceName);
        registration.put("Address", hostname);
        registration.put("Port", servicePort);

        Map<String, String> meta = new HashMap<>();
        meta.put("secure", "true");
        registration.put("Meta", meta);

        Map<String, Object> check = new HashMap<>();
        check.put("HTTP", String.format("%s://%s:%d%s", scheme, hostname, servicePort, healthCheckPath));
        check.put("Interval", healthCheckInterval);
        check.put("Timeout", healthCheckTimeout);
        check.put("DeregisterCriticalServiceAfter", healthCheckCriticalTimeout);
        check.put("TLSSkipVerify", true);
        registration.put("Check", check);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(registration, headers);

        log.info("Sending registration request to: {}", consulUrl);
        log.debug("Registration payload: {}", registration);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                consulUrl,
                HttpMethod.PUT,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("========================================");
                log.info("SUCCESS! Service registered with Consul");
                log.info("Instance ID: {}", instanceId);
                log.info("========================================");
            } else {
                log.error("Failed to register with Consul. Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Exception during registration: {}", e.getClass().getName());
            log.error("Message: {}", e.getMessage());

            // Check if Consul is reachable
            try {
                String healthUrl = String.format("http://%s:%d/v1/status/leader", consulHost, consulPort);
                ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);
                log.info("Consul is reachable. Leader: {}", healthResponse.getBody());
                log.error("Consul is UP but registration failed. Check the error above.");
            } catch (Exception consulCheckError) {
                log.error("Cannot reach Consul at http://{}:{}", consulHost, consulPort);
                log.error("Make sure:");
                log.error("  1. Consul is running locally");
                log.error("  2. SSH tunnel with -R 8500:localhost:8500 is active (if running on remote server)");
                log.error("  3. Consul is accessible from this host");
            }

            throw new RuntimeException("Registration failed", e);
        }
    }
}
