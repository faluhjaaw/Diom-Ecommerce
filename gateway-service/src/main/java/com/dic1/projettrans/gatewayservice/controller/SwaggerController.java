package com.dic1.projettrans.gatewayservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SwaggerController {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @GetMapping("/v3/api-docs/swagger-config")
    public Mono<ResponseEntity<Map<String, Object>>> swaggerConfig() {
        return Mono.just(ResponseEntity.ok(getSwaggerConfig()));
    }

    @GetMapping("/swagger-resources")
    public Mono<ResponseEntity<List<Map<String, String>>>> swaggerResources() {
        List<Map<String, String>> resources = new ArrayList<>();

        Flux<RouteDefinition> routeDefinitions = routeDefinitionLocator.getRouteDefinitions();

        routeDefinitions.subscribe(routeDefinition -> {
            String routeId = routeDefinition.getId();
            if (!routeId.equals("discovery-service")) {
                Map<String, String> resource = new HashMap<>();
                resource.put("name", routeId);
                resource.put("url", "/" + routeId + "/v3/api-docs");
                resource.put("swaggerVersion", "3.0");
                resources.add(resource);
            }
        });

        return Mono.just(ResponseEntity.ok(resources));
    }

    private Map<String, Object> getSwaggerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("configUrl", "/v3/api-docs/swagger-config");

        List<Map<String, String>> urls = new ArrayList<>();

        // Authentication Service
        urls.add(createUrl("Authentication Service", "/authentication/v3/api-docs"));

        // Customer Service
        urls.add(createUrl("Customer Service", "/customer-service/v3/api-docs"));

        // Product Service
        urls.add(createUrl("Product Service", "/product-service/v3/api-docs"));

        // Cart Service
        urls.add(createUrl("Cart Service", "/cart-service/v3/api-docs"));

        // Order Service
        urls.add(createUrl("Order Service", "/order-service/v3/api-docs"));

        // Avis Service
        urls.add(createUrl("Avis Service", "/avis-service/v3/api-docs"));

        config.put("urls", urls);
        return config;
    }

    private Map<String, String> createUrl(String name, String url) {
        Map<String, String> urlMap = new HashMap<>();
        urlMap.put("name", name);
        urlMap.put("url", url);
        return urlMap;
    }
}