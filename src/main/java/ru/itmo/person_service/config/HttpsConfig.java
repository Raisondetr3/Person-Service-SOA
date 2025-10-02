package ru.itmo.person_service.config;

import org.eclipse.jetty.server.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpsConfig {

    @Value("${http.redirect.port:58080}")
    private int httpRedirectPort;

    @Value("${server.port:58123}")
    private int httpsPort;

    @Bean
    public WebServerFactoryCustomizer<JettyServletWebServerFactory> jettyCustomizer() {
        return factory -> {
            factory.addServerCustomizers(server -> {
                HttpConfiguration httpConfig = new HttpConfiguration();
                httpConfig.setSecureScheme("https");
                httpConfig.setSecurePort(httpsPort);
                httpConfig.addCustomizer(new SecureRequestCustomizer());

                ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
                httpConnector.setPort(httpRedirectPort);

                server.addConnector(httpConnector);
            });
        };
    }
}