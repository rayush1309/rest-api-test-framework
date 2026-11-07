package com.sdetlabs.framework.mocking;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.sdetlabs.framework.http.RestClient;

import java.nio.file.Path;

/** Starts an isolated WireMock instance with this project's reusable JSON stub mappings. */
public final class WireMockTestServer implements AutoCloseable {
    private final WireMockServer server;

    public WireMockTestServer() {
        String mappingsDirectory = Path.of("src", "test", "resources", "wiremock")
                .toAbsolutePath().toString();
        server = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort()
                .usingFilesUnderDirectory(mappingsDirectory));
        server.start();
    }

    public WireMockServer wireMock() {
        return server;
    }

    public String baseUrl() {
        return server.baseUrl();
    }

    public RestClient client() {
        return new RestClient(baseUrl());
    }

    @Override
    public void close() {
        server.stop();
    }
}
