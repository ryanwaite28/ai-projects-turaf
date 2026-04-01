package com.turaf.architecture.config;

public class TestConfig {
    
    private static final String ENV = System.getProperty("karate.env", "local");
    
    public static String getEnvironment() {
        return ENV;
    }
    
    public static boolean isLocal() {
        return "local".equals(ENV);
    }
    
    public static boolean isDev() {
        return "dev".equals(ENV);
    }
    
    public static boolean isQa() {
        return "qa".equals(ENV);
    }
    
    public static boolean isProd() {
        return "prod".equals(ENV);
    }
}
