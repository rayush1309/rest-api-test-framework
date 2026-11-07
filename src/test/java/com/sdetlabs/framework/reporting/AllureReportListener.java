package com.sdetlabs.framework.reporting;

import com.sdetlabs.framework.logging.HttpExchangeRecorder;
import io.qameta.allure.Allure;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/** Adds sanitized request/response exchanges and failures to each Allure test result. */
public final class AllureReportListener implements ITestListener, IInvokedMethodListener {
    @Override
    public void onTestStart(ITestResult result) {
        HttpExchangeRecorder.beginTest(exchange -> Allure.attachment("Sanitized HTTP exchange", "text/plain", exchange));
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (method.isTestMethod()) {
            HttpExchangeRecorder.finishTest();
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        HttpExchangeRecorder.finishTest();
    }
}
