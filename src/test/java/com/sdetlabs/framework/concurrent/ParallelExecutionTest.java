package com.sdetlabs.framework.concurrent;

import com.sdetlabs.framework.mocking.WireMockTestServer;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** Local-only checks for parallel TestNG execution, thread isolation, and mock isolation. */
public class ParallelExecutionTest {
    private static final int PROVIDER_CASES = 6;
    private final AtomicInteger completedProviderCases = new AtomicInteger();

    @BeforeClass(alwaysRun = true)
    public void resetProviderCounter() {
        completedProviderCases.set(0);
    }

    @Test(groups = {"parallel", "concurrency", "regression"})
    public void executionContextIsIsolatedAcrossConcurrentThreads() throws Exception {
        int workers = 6;
        CyclicBarrier barrier = new CyclicBarrier(workers);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        List<Future<String>> values = new ArrayList<>();
        try {
            for (int worker = 0; worker < workers; worker++) {
                String expected = "worker-" + worker;
                values.add(executor.submit(() -> {
                    try {
                        ThreadLocalTestContext.put("requestId", expected);
                        barrier.await(5, TimeUnit.SECONDS);
                        return ThreadLocalTestContext.get("requestId", String.class).orElseThrow();
                    } finally {
                        ThreadLocalTestContext.clear();
                    }
                }));
            }
            for (int index = 0; index < workers; index++) {
                assertEquals(values.get(index).get(5, TimeUnit.SECONDS), "worker-" + index);
            }
            assertTrue(ThreadLocalTestContext.get("requestId", String.class).isEmpty(),
                    "Worker context must not be visible to the TestNG thread");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test(groups = {"parallel", "concurrency", "regression"})
    public void listenerClearsContextAfterEachInvocation() {
        assertTrue(ThreadLocalTestContext.get("testInvocation", String.class).isEmpty(),
                "Listener should clear values after the previous invocation");
        ThreadLocalTestContext.put("testInvocation", "temporary");
    }

    @Test(dataProvider = "isolatedMockCases", groups = {"parallel", "mocking", "regression"})
    public void parallelDataProviderCasesUseIsolatedWireMockServers(String caseId) {
        try (WireMockTestServer mock = new WireMockTestServer()) {
            mock.wireMock().stubFor(get(urlEqualTo("/parallel-case"))
                    .withHeader("X-Case-Id", equalTo(caseId))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody("{\"caseId\":\"" + caseId + "\"}")));

            Response response = mock.client().get("/parallel-case", mock.client().newRequest()
                    .header("X-Case-Id", caseId));
            assertEquals(response.statusCode(), 200);
            assertEquals(response.jsonPath().getString("caseId"), caseId);
            mock.wireMock().verify(1, getRequestedFor(urlEqualTo("/parallel-case"))
                    .withHeader("X-Case-Id", equalTo(caseId)));
            completedProviderCases.incrementAndGet();
        }
    }

    @Test(dependsOnMethods = "parallelDataProviderCasesUseIsolatedWireMockServers",
            groups = {"parallel", "concurrency", "regression"})
    public void allParallelDataProviderCasesCompleted() {
        assertEquals(completedProviderCases.get(), PROVIDER_CASES);
    }

    @DataProvider(name = "isolatedMockCases", parallel = true)
    public Object[][] isolatedMockCases() {
        return new Object[][]{{"case-a"}, {"case-b"}, {"case-c"}, {"case-d"}, {"case-e"}, {"case-f"}};
    }
}
