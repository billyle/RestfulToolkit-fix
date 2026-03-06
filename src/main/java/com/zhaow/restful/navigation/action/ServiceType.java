package com.zhaow.restful.navigation.action;

public enum ServiceType {
    REST("Rest"),
    FEIGN("Feign"),
    ALL("All");
    
    private final String displayName;
    
    ServiceType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}