package com.zhaow.restful.annotations;

public enum FeignClientAnnotation {
    FEIGN_CLIENT("FeignClient", "org.springframework.cloud.openfeign.FeignClient");

    FeignClientAnnotation(String shortName, String qualifiedName) {
        this.shortName = shortName;
        this.qualifiedName = qualifiedName;
    }

    private String shortName;
    private String qualifiedName;

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getShortName() {
        return shortName;
    }

    public static FeignClientAnnotation getByQualifiedName(String qualifiedName) {
        for (FeignClientAnnotation annotation : FeignClientAnnotation.values()) {
            if (annotation.getQualifiedName().equals(qualifiedName)) {
                return annotation;
            }
        }
        return null;
    }

    public static FeignClientAnnotation getByShortName(String shortName) {
        for (FeignClientAnnotation annotation : FeignClientAnnotation.values()) {
            if (annotation.getShortName().equals(shortName)) {
                return annotation;
            }
        }
        return null;
    }
}