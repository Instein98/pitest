/*
 * Copyright 2010 Henry Coles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.mutationtest.execute;

import org.pitest.classinfo.ClassName;
import org.pitest.functional.F3;
import org.pitest.mutationtest.DetectionStatus;
import org.pitest.mutationtest.MutationStatusTestPair;
import org.pitest.mutationtest.engine.*;
import org.pitest.mutationtest.mocksupport.JavassistInterceptor;
import org.pitest.testapi.TestResult;
import org.pitest.testapi.TestUnit;
import org.pitest.testapi.execute.Container;
import org.pitest.testapi.execute.ExitingResultCollector;
import org.pitest.testapi.execute.MultipleTestGroup;
import org.pitest.testapi.execute.Pitest;
import org.pitest.testapi.execute.containers.ConcreteResultCollector;
import org.pitest.testapi.execute.containers.UnContainer;
import org.pitest.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.pitest.util.Unchecked.translateCheckedException;

public class MutationTestWorker {

  private static final Logger                               LOG   = Log
      .getLogger();

  // micro optimise debug logging
  private static final boolean                              DEBUG = LOG
      .isLoggable(Level.FINE);

  private final Mutater                                     mutater;
  private final ClassLoader                                 loader;
  private final F3<ClassName, ClassLoader, byte[], Boolean> hotswap;
  private final boolean                                     fullMutationMatrix;

  public MutationTestWorker(
      final F3<ClassName, ClassLoader, byte[], Boolean> hotswap,
      final Mutater mutater, final ClassLoader loader, final boolean fullMutationMatrix) {
    this.loader = loader;
    this.mutater = mutater;
    this.hotswap = hotswap;
    this.fullMutationMatrix = fullMutationMatrix;
  }

  protected void run(final Collection<MutationDetails> range, final Reporter r,
      final TimeOutDecoratedTestSource testSource) throws IOException {

    for (final MutationDetails mutation : range) {
      if (DEBUG) {
        LOG.fine("Running mutation " + mutation);
      }
      final long t0 = System.currentTimeMillis();
      processMutation(r, testSource, mutation);
      if (DEBUG) {
        LOG.fine("processed mutation in " + (System.currentTimeMillis() - t0)
            + " ms.");
      }
    }
  }

  @Deprecated
  private void storeMutationDetails(String filePath, Collection<MutationDetails> range)
  {
    try {
      List<SimplifiedMutationDetails> simplifiedMutationDetailsList = new ArrayList<>();
      for (MutationDetails md : range)
      {
        simplifiedMutationDetailsList.add(new SimplifiedMutationDetails(md));
      }
      FileOutputStream fileOut = new FileOutputStream(filePath);
      ObjectOutputStream out = new ObjectOutputStream(fileOut);
      out.writeObject(simplifiedMutationDetailsList);
      out.close();
      fileOut.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      // System.exit(0);
    }

  }

  private void processMutation(final Reporter r,
      final TimeOutDecoratedTestSource testSource,
      final MutationDetails mutationDetails) throws IOException {

    long t0 = System.currentTimeMillis();

    final MutationIdentifier mutationId = mutationDetails.getId();
    final Mutant mutatedClass = this.mutater.getMutation(mutationId);

    // For the benefit of mocking frameworks such as PowerMock
    // mess with the internals of Javassist so our mutated class
    // bytes are returned
    JavassistInterceptor.setMutant(mutatedClass);

    if (DEBUG) {
      LOG.fine("mutating method " + mutatedClass.getDetails().getMethod());
    }
    final List<TestUnit> relevantTests = testSource
        .translateTests(mutationDetails.getTestsInOrder());

    r.describe(mutationId);

    final MutationStatusTestPair mutationDetected = handleMutation(
        mutationDetails, mutatedClass, relevantTests);

    mutationDetails.setDescTestTimeMap(mutatedClass.getDetails().getDescTestsTimeMap());

    long patchExecutionTime = System.currentTimeMillis() - t0;
    mutationDetected.setMutationExecutionTime(patchExecutionTime);
    r.report(mutationId, mutationDetected);
    if (DEBUG) {
      LOG.fine("Mutation " + mutationId + " detected = " + mutationDetected);
    }
  }

  private MutationStatusTestPair handleMutation(
      final MutationDetails mutationId, final Mutant mutatedClass,
      final List<TestUnit> relevantTests) {
    MutationStatusTestPair mutationDetected;
    if ((relevantTests == null) || relevantTests.isEmpty()) {
      LOG.info("No test coverage for mutation  " + mutationId + " in "
          + mutatedClass.getDetails().getMethod());
      mutationDetected = new MutationStatusTestPair(0,
          DetectionStatus.RUN_ERROR);
    } else {
      mutationDetected = handleCoveredMutation(mutationId, mutatedClass,
          relevantTests);

    }
    return mutationDetected;
  }

  private MutationStatusTestPair handleCoveredMutation(
      final MutationDetails mutationId, final Mutant mutatedClass,
      final List<TestUnit> relevantTests) {
    MutationStatusTestPair mutationDetected;
    if (DEBUG) {
      LOG.fine("" + relevantTests.size() + " relevant test for "
          + mutatedClass.getDetails().getMethod());
    }

    final Container c = createNewContainer();
    final long t0 = System.currentTimeMillis();
    if (this.hotswap.apply(mutationId.getClassName(), this.loader,
        mutatedClass.getBytes())) {
      if (DEBUG) {
        LOG.fine("replaced class with mutant in "
            + (System.currentTimeMillis() - t0) + " ms");
      }
      mutationDetected = doTestsDetectMutation(c, relevantTests, mutatedClass);
    } else {
      LOG.warning("Mutation " + mutationId + " was not viable ");
      mutationDetected = new MutationStatusTestPair(0,
          DetectionStatus.NON_VIABLE);
    }
    return mutationDetected;
  }

  private static Container createNewContainer() {
    final Container c = new UnContainer() {
      @Override
      public List<TestResult> execute(final TestUnit group) {
        List<TestResult> results = new ArrayList<>();
        final ExitingResultCollector rc = new ExitingResultCollector(
            new ConcreteResultCollector(results));
        group.execute(rc);
        return results;
      }
    };
    return c;
  }



  @Override
  public String toString() {
    return "MutationTestWorker [mutater=" + this.mutater + ", loader="
        + this.loader + ", hotswap=" + this.hotswap + "]";
  }

  private MutationStatusTestPair doTestsDetectMutation(final Container c,
                                                       final List<TestUnit> tests, Mutant mutedClass) {
    try {
      final CheckTestHasFailedResultWithTimingListener listener = new CheckTestHasFailedResultWithTimingListener(fullMutationMatrix);

      final Pitest pit = new Pitest(listener);

      if (this.fullMutationMatrix) {
        pit.run(c, tests, mutedClass);
      } else {
        pit.run(c, createEarlyExitTestGroup(tests), mutedClass);
      }
      listener.setDescTestsExecutionTime(mutedClass.getDetails().getDescTestsTimeMap());

      return createStatusTestPair(listener);
    } catch (final Exception ex) {
      throw translateCheckedException(ex);
    }

  }

  private MutationStatusTestPair createStatusTestPair(
          final CheckTestHasFailedResultListener listener) {
    List<String> failingTests = listener.getFailingTests().stream()
            .map(description -> description.getQualifiedName()).collect(Collectors.toList());
    List<String> succeedingTests = listener.getSucceedingTests().stream()
            .map(description -> description.getQualifiedName()).collect(Collectors.toList());
    return new MutationStatusTestPair(listener.getNumberOfTestsRun(),
            listener.status(), failingTests, succeedingTests);
  }

  // Todo
  private MutationStatusTestPair createStatusTestPair(
          final CheckTestHasFailedResultWithTimingListener listener) {
    List<String> failingTests = listener.getFailingTests().stream()
            .map(description -> description.getQualifiedName()).collect(Collectors.toList());
    List<String> succeedingTests = listener.getSucceedingTests().stream()
            .map(description -> description.getQualifiedName())
            .filter(x -> !x.startsWith("testExecutionTime:")).collect(Collectors.toList());
    MutationStatusTestPair mstp = new MutationStatusTestPair(listener.getNumberOfTestsRun(),
            listener.status(), failingTests, succeedingTests);
    mstp.setDescTestsExecutionTime(listener.getDescTestsExecutionTime());
    return mstp;
  }

  private List<TestUnit> createEarlyExitTestGroup(final List<TestUnit> tests) {
    return Collections.<TestUnit> singletonList(new MultipleTestGroup(tests));
  }

}
