/*
 * Copyright 2012 Henry Coles
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

package org.pitest.coverage;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.pitest.Description;
import org.pitest.classinfo.ClassInfo;
import org.pitest.classinfo.ClassName;
import org.pitest.classinfo.CodeSource;
import org.pitest.coverage.domain.TestInfo;
import org.pitest.coverage.execute.CoverageResult;
import org.pitest.functional.F2;
import org.pitest.functional.FCollection;
import org.pitest.functional.Option;
import org.pitest.mutationtest.instrument.ClassLine;
import org.pitest.util.Log;

public class CoverageData implements CoverageDatabase {

  private final static Logger                              LOG           = Log
                                                                             .getLogger();

  private final Map<String, Map<ClassLine, Set<TestInfo>>> classCoverage = new LinkedHashMap<String, Map<ClassLine, Set<TestInfo>>>();
  private final CodeSource                                 code;

  private boolean                                          hasFailedTest = false;

  CoverageData(final CodeSource code) {
    this.code = code;
  }

  public Collection<TestInfo> getTestsForClassLine(final ClassLine classLine) {
    final Collection<TestInfo> result = getTestsForJVMClassName(
        classLine.getJVMClassName()).get(classLine);
    if (result == null) {
      return Collections.emptyList();
    } else {
      return result;
    }
  }

  public boolean allTestsGreen() {
    return !this.hasFailedTest;
  }

  public Collection<ClassInfo> getClassInfo(final Collection<String> classes) {
    return this.code.getClassInfo(classes);
  }

  public int getNumberOfCoveredLines(final Collection<String> mutatedClass) {
    return FCollection.fold(numberCoveredLines(), 0, mutatedClass);
  }

  public Collection<TestInfo> getTestsForClass(final String clazz) {
    final Map<ClassLine, Set<TestInfo>> map = getTestsForJVMClassName(clazz);

    final Set<TestInfo> tis = new LinkedHashSet<TestInfo>(map.values().size());
    for (final Set<TestInfo> each : map.values()) {
      tis.addAll(each);
    }
    return tis;

  }

  void calculateClassCoverage(final CoverageResult cr) {

    checkForFailedTest(cr);
    final TestInfo ti = this.createTestInfo(cr.getTestUnitDescription(),
        cr.getExecutionTime());

    for (final ClassStatistics i : cr.getCoverage()) {
      final Map<ClassLine, Set<TestInfo>> map = getCoverageMapForClass(i
          .getClassName());
      mapTestsToClassLines(ti, i, map);
    }
  }

  private void checkForFailedTest(final CoverageResult cr) {
    if (!cr.isGreenTest()) {
      recordTestFailure();
      LOG.warning(cr.getTestUnitDescription()
          + " did not pass without mutation.");
    }
  }

  private void mapTestsToClassLines(final TestInfo test,
      final ClassStatistics stats, final Map<ClassLine, Set<TestInfo>> map) {

    for (final int line : stats.getUniqueVisitedLines()) {
      final ClassLine key = new ClassLine(stats.getClassName(), line);
      Set<TestInfo> testsForLine = map.get(key);
      if (testsForLine == null) {
        testsForLine = new TreeSet<TestInfo>(new TestInfoNameComparator()); // inject
        // comparator
        // here
        map.put(key, testsForLine);
      }
      testsForLine.add(test);

    }
  }


  private TestInfo createTestInfo(final Description description,
      final int executionTime) {
    final Option<ClassName> testee = this.code.findTestee(description
        .getFirstTestClass());
    return new TestInfo(description.getFirstTestClass(),
        description.getQualifiedName(), executionTime, testee);
  }

  private F2<Integer, String, Integer> numberCoveredLines() {
    return new F2<Integer, String, Integer>() {

      public Integer apply(final Integer a, final String clazz) {
        return a + getNumberOfCoveredLines(clazz);
      }

    };
  }

  private int getNumberOfCoveredLines(final String clazz) {
    final Map<ClassLine, Set<TestInfo>> map = this.classCoverage.get(clazz
        .replace(".", "/"));
    if (map != null) {
      return map.size();
    } else {
      return 0;
    }

  }

  private Map<ClassLine, Set<TestInfo>> getTestsForJVMClassName(
      final String clazz) {
    // Use any test that provided some coverage of the class
    // This fails to consider tests that only accessed a static variable
    // of the class in question as this does not register as coverage.

    Map<ClassLine, Set<TestInfo>> map = this.classCoverage.get(clazz);
    if (map == null) {
      map = new LinkedHashMap<ClassLine, Set<TestInfo>>(0);
    }
    return map;
  }

  private void recordTestFailure() {
    this.hasFailedTest = true;
  }

  private Map<ClassLine, Set<TestInfo>> getCoverageMapForClass(
      final String className) {
    Map<ClassLine, Set<TestInfo>> map = this.classCoverage.get(className);
    if (map == null) {
      map = new LinkedHashMap<ClassLine, Set<TestInfo>>(0);
      this.classCoverage.put(className, map);
    }
    return map;
  }

}