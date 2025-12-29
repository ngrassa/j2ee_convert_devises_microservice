package com.example.rates;

import org.glassfish.jersey.server.ResourceConfig;

public class AppConfig extends ResourceConfig {
    public AppConfig() {
        packages("com.example.rates");
        register(CorsFilter.class);
    }
}
