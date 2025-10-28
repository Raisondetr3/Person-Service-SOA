package ru.itmo.person_service.config;

import com.ecwid.consul.v1.agent.model.NewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistrationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ConsulConfig {

    @Bean
    @ConditionalOnProperty(value = "spring.cloud.consul.discovery.enabled", matchIfMissing = true)
    public ConsulRegistrationCustomizer consulRegistrationCustomizer() {
        return registration -> {
            NewService service = registration.getService();
            NewService.Check check = service.getCheck();

            if (check != null) {
                check.setTlsSkipVerify(true);
                log.info("Consul health check configured with TLS skip verification enabled");
                log.info("Health check URL: {}", check.getHttp());
            }
        };
    }
}
