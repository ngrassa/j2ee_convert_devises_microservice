package com.example.converter;

import org.glassfish.jersey.server.ResourceConfig;

public class AppConfig extends ResourceConfig {
    public AppConfig() {
        packages("com.example.converter");
        register(CorsFilter.class);
    }
}
