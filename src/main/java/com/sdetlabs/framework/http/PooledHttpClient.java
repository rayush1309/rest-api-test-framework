package com.sdetlabs.framework.http;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

/** Apache HTTP transport with explicit total/per-route connection and timeout limits. */
public final class PooledHttpClient implements AutoCloseable {
    private final PoolingHttpClientConnectionManager connectionManager;
    private final CloseableHttpClient client;

    public PooledHttpClient(int maxTotalConnections, int maxConnectionsPerRoute,
                            Duration poolAcquireTimeout, Duration connectTimeout, Duration responseTimeout) {
        if (maxTotalConnections < 1) throw new IllegalArgumentException("maxTotalConnections must be at least 1");
        if (maxConnectionsPerRoute < 1 || maxConnectionsPerRoute > maxTotalConnections) {
            throw new IllegalArgumentException("maxConnectionsPerRoute must be between 1 and maxTotalConnections");
        }
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeoutMillis(poolAcquireTimeout, "poolAcquireTimeout"))
                .setConnectTimeout(timeoutMillis(connectTimeout, "connectTimeout"))
                .setSocketTimeout(timeoutMillis(responseTimeout, "responseTimeout"))
                .build();
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public HttpResult get(String url) throws IOException {
        Objects.requireNonNull(url, "url must not be null");
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = client.execute(request)) {
            HttpEntity entity = response.getEntity();
            String body = entity == null ? "" : EntityUtils.toString(entity);
            return new HttpResult(response.getStatusLine().getStatusCode(), body);
        }
    }

    public int leasedConnections() {
        return connectionManager.getTotalStats().getLeased();
    }

    public int maxTotalConnections() {
        return connectionManager.getMaxTotal();
    }

    public int maxConnectionsPerRoute() {
        return connectionManager.getDefaultMaxPerRoute();
    }

    private static int timeoutMillis(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " must not be null");
        if (duration.isNegative() || duration.isZero()) throw new IllegalArgumentException(name + " must be positive");
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, duration.toMillis()));
    }

    @Override
    public void close() throws IOException {
        try {
            client.close();
        } finally {
            connectionManager.close();
        }
    }

    public record HttpResult(int statusCode, String body) { }
}
