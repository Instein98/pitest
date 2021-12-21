/*
 * Copyright 2011 Henry Coles
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
package org.pitest.mutationtest.report.xml;

import org.pitest.mutationtest.ClassMutationResults;
import org.pitest.mutationtest.MutationResult;
import org.pitest.mutationtest.MutationResultListener;
import org.pitest.mutationtest.MutationStatusTestPair;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.testapi.Description;
import org.pitest.util.ResultOutputStrategy;
import org.pitest.util.StringUtil;
import org.pitest.util.Unchecked;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;


enum Tag {
  mutation, sourceFile, mutatedClass, mutatedMethod, methodDescription, lineNumber, mutator, index, killingTest, killingTests, succeedingTests, description, block, timeoutTests, runErrorTests, memoryErrorTests,
  patchExecutionTime, test, time, name, testsExecution, testStatus
}

public class XMLReportListener implements MutationResultListener {

  public static final String MUTATION_MATRIX_TEST_SEPARATOR = "|";

  private final Writer out;
  private final boolean fullMutationMatrix;

  public XMLReportListener(final ResultOutputStrategy outputStrategy, boolean fullMutationMatrix) {
    this(outputStrategy.createWriterForFile("mutations.xml"), fullMutationMatrix);
  }

  public XMLReportListener(final Writer out, boolean fullMutationMatrix) {
    this.out = out;
    this.fullMutationMatrix = fullMutationMatrix;
  }

  private void writeResult(final ClassMutationResults metaData) {
    for (final MutationResult mutation : metaData.getMutations()) {
      writeMutationResultXML(mutation);
    }
  }

  private void writeMutationResultXML(final MutationResult result) {
    write(makeNode(makeMutationNode(result), makeMutationAttributes(result),
        Tag.mutation) + "\n");
  }

  private String makeMutationAttributes(final MutationResult result) {
    return "detected='" + result.getStatus().isDetected() + "' status='"
        + result.getStatus() + "' numberOfTestsRun='"
        + result.getNumberOfTestsRun() + "'";
  }

  private String makeMutationNode(final MutationResult mutation) {
    final MutationDetails details = mutation.getDetails();
    MutationStatusTestPair mstp = mutation.getStatusTestPair();
    return makeNode(clean(details.getFilename()), Tag.sourceFile)
            + makeNode(clean(details.getClassName().asJavaName()), Tag.mutatedClass)
            + makeNode(clean(details.getMethod().name()), Tag.mutatedMethod)
            + makeNode(clean(details.getId().getLocation().getMethodDesc()),
            Tag.methodDescription)
            + makeNode("" + details.getLineNumber(), Tag.lineNumber)
            + makeNode(clean(details.getMutator()), Tag.mutator)
            + makeNode("" + details.getFirstIndex(), Tag.index)
            + makeNode("" + details.getBlock(), Tag.block)
            + makeNodeWhenConditionSatisfied(!fullMutationMatrix,
            createKillingTestDesc(mutation.getKillingTest()), Tag.killingTest)
            + makeNodeWhenConditionSatisfied(fullMutationMatrix,
            createTestDesc(mutation.getKillingTests()), Tag.killingTests)
            + makeNodeWhenConditionSatisfied(fullMutationMatrix,
            createTestDesc(mutation.getSucceedingTests()), Tag.succeedingTests)
            + makeNodeWhenConditionSatisfied(fullMutationMatrix,
            createTestDesc(mutation.getTimeoutTests()), Tag.timeoutTests)
            + makeNodeWhenConditionSatisfied(fullMutationMatrix,
            createTestDesc(mutation.getRunErrorTests()), Tag.runErrorTests)
            + makeNodeWhenConditionSatisfied(fullMutationMatrix,
            createTestDesc(mutation.getMemoryErrorTests()), Tag.memoryErrorTests)
            + makeNode(clean(details.getDescription()), Tag.description)
            + makeNode(makeTestExecutionNodes(mutation, mstp), Tag.testsExecution);
//            + makeNode(String.valueOf(mstp.getMutationExecutionTime()) + "ms", patchExecutionTime);
  }

  private String clean(final String value) {
    return StringUtil.escapeBasicHtmlChars(value);
  }

  private String makeNode(final String value, final String attributes,
      final Tag tag) {
    if (value != null) {
      return "<" + tag + " " + attributes + ">" + value + "</" + tag + ">";
    } else {
      return "<" + tag + attributes + "/>";
    }

  }

  private String makeNodeWhenConditionSatisfied(final boolean condition, final String value, final Tag tag) {
    if (!condition) {
      return "";
    }
    return makeNode(value, tag);
  }

  private String makeNode(final String value, final Tag tag) {
    if (value != null) {
      return "<" + tag + ">" + value + "</" + tag + ">";
    } else {
      return "<" + tag + "/>";
    }
  }

  private String createKillingTestDesc(final Optional<String> killingTest) {
    if (killingTest.isPresent()) {
      return createTestDesc(Arrays.asList(killingTest.get()));
    } else {
      return null;
    }
  }

  private String createTestDesc(final List<String> tests) {
    if (tests.isEmpty()) {
      return "";
    }

    StringBuilder builder = new StringBuilder();

    for (String test : tests) {
      String nameNode = makeNode(test, Tag.name);
      String testNode = makeNode(nameNode, Tag.test);
      builder.append(testNode);
//      builder.append(test);
//      builder.append(MUTATION_MATRIX_TEST_SEPARATOR);
    }

    // remove last separator
//    builder.setLength(builder.length() - 1);

//    return clean(builder.toString());
    return builder.toString();
  }

  private void write(final String value) {
    try {
      this.out.write(value);
    } catch (final IOException e) {
      throw Unchecked.translateCheckedException(e);
    }
  }

  private String makeTestExecutionNodes(MutationResult mutation, MutationStatusTestPair mstp) {
    List<String> killed = mutation.getKillingTests();
    List<String> succeed = mutation.getSucceedingTests();
    List<String> timeout = mutation.getTimeoutTests();
    List<String> runErr = mutation.getRunErrorTests();
    List<String> memErr = mutation.getMemoryErrorTests();
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Description, Long>entry : mstp.getTestsExecutionTime().entrySet()) {
      String testName = entry.getKey().getQualifiedName();
      String nameNode = makeNode(testName, Tag.name);
      String statusNode;
      if (killed.contains(testName)) {
        statusNode = makeNode("FAIL", Tag.testStatus);
      } else if (succeed.contains(testName)) {
        statusNode = makeNode("PASS", Tag.testStatus);
      } else if (timeout.contains(testName)) {
        statusNode = makeNode("TIMEOUT", Tag.testStatus);
      } else if (runErr.contains(testName)) {
        statusNode = makeNode("RUNTIME_ERROR", Tag.testStatus);
      } else if (memErr.contains(testName)) {
        statusNode = makeNode("MEMORY_ERROR", Tag.testStatus);
      } else {
        statusNode = makeNode("UNKNOWN", Tag.testStatus);
      }
      String timeNode = makeNode(entry.getValue().toString() + "ms", Tag.time);
      String testNode = makeNode(nameNode + statusNode + timeNode, Tag.test);
      sb.append(testNode);
    }
    return sb.toString();
  }

  /**
   * Prepare to make test time nodes
   * @author: Jun Yang
   */
  private String getRunTestNodes(Map<Description, Long> testTimeMap) {
    String testNodes = "";
    long t0 = System.currentTimeMillis();

    for (Map.Entry<Description, Long>entry : testTimeMap.entrySet()) {
      String nameNode = makeNode(entry.getKey().getQualifiedName(), Tag.name);
      String timeNode = makeNode(entry.getValue().toString() + "ms", Tag.time);
      String testNode = makeNode(nameNode + timeNode, Tag.test);
      testNodes += testNode;
    }
    return testNodes;
  }

  @Override
  public void runStart() {
    write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    write("<mutations>\n");
  }

  @Override
  public void handleMutationResult(final ClassMutationResults metaData) {
    writeResult(metaData);
  }

  @Override
  public void runEnd() {
    try {
      write("</mutations>\n");
      this.out.close();
    } catch (final IOException e) {
      throw Unchecked.translateCheckedException(e);
    }
  }

}
