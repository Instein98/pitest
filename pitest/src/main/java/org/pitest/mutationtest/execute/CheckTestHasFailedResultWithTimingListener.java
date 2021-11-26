package org.pitest.mutationtest.execute;

import org.pitest.functional.Option;
import org.pitest.mutationtest.DetectionStatus;
import org.pitest.testapi.Description;
import org.pitest.testapi.TestResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckTestHasFailedResultWithTimingListener extends CheckTestHasFailedResultListener{

    private final List<Description> succeedingTests = new ArrayList<>();
    private final List<Description>   failingTests = new ArrayList<>();
    private final boolean       recordPassingTests;
    private int                 testsRun        = 0;
    private Map<Description, Long>       testsDescExecutionTimeMap  = new HashMap<>();

    public CheckTestHasFailedResultWithTimingListener(boolean recordPassingTests) {
        super(recordPassingTests);
        this.recordPassingTests = recordPassingTests;
    }

    @Override
    public void onTestFailure(final TestResult tr) {
        this.failingTests.add(tr.getDescription());
    }

    @Override
    public void onTestSkipped(final TestResult tr) {

    }

    @Override
    public void onTestStart(final Description d) {
        this.testsRun++;
    }

    @Override
    public void onTestSuccess(final TestResult tr) {
        if (recordPassingTests) {
            this.succeedingTests.add(tr.getDescription());
        }
    }


    public DetectionStatus status() {
        if (!this.failingTests.isEmpty()) {
            return DetectionStatus.KILLED;
        } else {
            return DetectionStatus.SURVIVED;
        }
    }

    public List<Description> getSucceedingTests() {
        return succeedingTests;
    }

    public List<Description> getFailingTests() {
        return failingTests;
    }

    public int getNumberOfTestsRun() {
        return this.testsRun;
    }

    @Override
    public void onRunEnd() {

    }

    @Override
    public void onRunStart() {

    }

    public void setDescTestsExecutionTime(Map<Description, Long> testsDescExecutionTimeMap)
    {
        this.testsDescExecutionTimeMap = testsDescExecutionTimeMap;
    }

    public Map<Description, Long> getDescTestsExecutionTime()
    {
        return this.testsDescExecutionTimeMap;
    }

}
