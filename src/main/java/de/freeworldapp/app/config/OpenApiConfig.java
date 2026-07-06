package de.freeworldapp.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI freeworldOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FreeWorld API")
                        .description("Community marketplace for a gift economy. "
                                + "Mutating requests and sensitive reads require the X-Session-Token header issued at login.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes("sessionToken",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Session-Token")));
    }
}
