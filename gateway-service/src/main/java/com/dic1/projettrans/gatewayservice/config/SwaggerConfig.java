package com.dic1.projettrans.gatewayservice.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {
    @Bean
    public List<GroupedOpenApi> apis(RouteDefinitionLocator locator) {
        List<GroupedOpenApi> groups = new ArrayList<>();

        // Service d'authentification
        groups.add(GroupedOpenApi.builder()
                .group("authentication-service")
                .pathsToMatch("/authentication/**")
                .build());

        // Service client
        groups.add(GroupedOpenApi.builder()
                .group("customer-service")
                .pathsToMatch("/customer-service/**")
                .build());

        // Service produit
        groups.add(GroupedOpenApi.builder()
                .group("product-service")
                .pathsToMatch("/product-service/**")
                .build());

        // Service panier
        groups.add(GroupedOpenApi.builder()
                .group("cart-service")
                .pathsToMatch("/cart-service/**")
                .build());

        // Service commandes
        groups.add(GroupedOpenApi.builder()
                .group("order-service")
                .pathsToMatch("/order-service/**")
                .build());

        // Service avis
        groups.add(GroupedOpenApi.builder()
                .group("avis-service")
                .pathsToMatch("/avis-service/**")
                .build());

        return groups;
    }

}
