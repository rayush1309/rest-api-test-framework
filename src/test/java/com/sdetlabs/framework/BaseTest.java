package com.sdetlabs.framework;

import com.sdetlabs.framework.config.ConfigManager;
import com.sdetlabs.framework.http.RestClient;
import org.testng.annotations.BeforeClass;

/** Shared test setup. Clients remain test-owned and do not keep mutable request state. */
public abstract class BaseTest {
    protected ConfigManager config;

    @BeforeClass(alwaysRun = true)
    public void initializeFramework() {
        config = new ConfigManager();
    }

    protected RestClient clientFor(String service) {
        return RestClient.forService(config, service);
    }
}
