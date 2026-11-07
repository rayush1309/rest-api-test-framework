package com.sdetlabs.framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/** Loads environment defaults and overlays system properties and environment variables. */
public final class ConfigManager {
    private static final String DEFAULT_ENVIRONMENT = "qa";
    private final String environment;
    private final Properties properties = new Properties();

    public ConfigManager() {
        this(System.getProperty("env", DEFAULT_ENVIRONMENT));
    }

    public ConfigManager(String environment) {
        this.environment = requireEnvironment(environment);
        String resource = "config/" + this.environment + ".properties";
        try (InputStream stream = ConfigManager.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new FrameworkConfigurationException("No configuration found for environment '" + this.environment
                        + "' (expected classpath resource " + resource + ")");
            }
            properties.load(stream);
        } catch (IOException exception) {
            throw new FrameworkConfigurationException("Unable to load configuration resource " + resource, exception);
        }
    }

    public String environment() {
        return environment;
    }

    public String getRequired(String key) {
        Objects.requireNonNull(key, "Configuration key must not be null");
        String value = overrideValue(key);
        if (value == null || value.isBlank()) {
            throw new FrameworkConfigurationException("Missing required configuration key '" + key + "'");
        }
        return value.trim();
    }

    public String getBaseUrl(String service) {
        String key = service.toLowerCase(Locale.ROOT) + ".base.url";
        String value = getRequired(key);
        try {
            URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("URL must use HTTP or HTTPS and include a host");
            }
            return value.replaceAll("/+$", "");
        } catch (IllegalArgumentException exception) {
            throw new FrameworkConfigurationException("Invalid URL for configuration key '" + key + "': " + value,
                    exception);
        }
    }

    private String overrideValue(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            return systemValue;
        }
        String envName = key.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9]", "_").toUpperCase(Locale.ROOT);
        String environmentValue = System.getenv(envName);
        return environmentValue != null ? environmentValue : properties.getProperty(key);
    }

    private static String requireEnvironment(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]+")) {
            throw new FrameworkConfigurationException("Environment name must contain only letters, digits, '_' or '-'");
        }
        return value;
    }
}
