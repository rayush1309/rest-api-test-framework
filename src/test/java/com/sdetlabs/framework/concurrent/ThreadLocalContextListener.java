package com.sdetlabs.framework.concurrent;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/** Clears per-thread test state before and after each TestNG test invocation. */
public final class ThreadLocalContextListener implements IInvokedMethodListener {
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult result) {
        if (method.isTestMethod()) ThreadLocalTestContext.clear();
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (method.isTestMethod()) ThreadLocalTestContext.clear();
    }
}
