package com.sdetlabs.framework.config;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class ConfigManagerTest {
    @Test(groups = {"positive"})
    public void systemPropertyOverridesEnvironmentDefaults() {
        String key = "dummyjson.base.url";
        String previous = System.getProperty(key);
        try {
            System.setProperty(key, "https://override.example/api/");
            assertEquals(new ConfigManager("qa").getBaseUrl("dummyjson"), "https://override.example/api");
        } finally {
            if (previous == null) System.clearProperty(key);
            else System.setProperty(key, previous);
        }
    }

    @Test(groups = {"negative"})
    public void rejectsUnknownEnvironment() {
        assertThrows(FrameworkConfigurationException.class, () -> new ConfigManager("missing"));
    }

    @Test(groups = {"negative"})
    public void rejectsInvalidBaseUrl() {
        String key = "dummyjson.base.url";
        String previous = System.getProperty(key);
        try {
            System.setProperty(key, "file:///etc/passwd");
            assertThrows(FrameworkConfigurationException.class, () -> new ConfigManager("qa").getBaseUrl("dummyjson"));
        } finally {
            if (previous == null) System.clearProperty(key);
            else System.setProperty(key, previous);
        }
    }
}
