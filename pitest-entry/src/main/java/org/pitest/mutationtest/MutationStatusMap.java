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
package org.pitest.mutationtest;

import org.pitest.functional.FCollection;
import org.pitest.mutationtest.engine.MutationDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.pitest.functional.prelude.Prelude.or2;
import static org.pitest.functional.prelude.Prelude.putToMap;


public class MutationStatusMap {

  private final Map<MutationDetails, MutationStatusTestPair> mutationMap = new HashMap<>();

  public void setStatusForMutation(final MutationDetails mutation,
      final DetectionStatus status) {
    this.setStatusForMutations(Collections.singleton(mutation), status);
  }

  public void setStatusForMutation(final MutationDetails mutation,
      final MutationStatusTestPair status) {
    this.mutationMap.put(mutation, status);
  }

  public void setStatusForMutations(
      final Collection<MutationDetails> mutations, final DetectionStatus status) {
    FCollection.forEach(mutations,
        putToMap(this.mutationMap, new MutationStatusTestPair(0, status)));
  }

  public List<MutationResult> createMutationResults() {
    return FCollection.map(this.mutationMap.entrySet(),
        detailsToMutationResults());

  }

  public boolean hasUnrunMutations() {
    return !getUnrunMutations().isEmpty();
  }

  public Collection<MutationDetails> getUnrunMutations() {
    return this.mutationMap.entrySet().stream()
            .filter(hasStatus(DetectionStatus.NOT_STARTED))
            .map(toMutationDetails())
            .collect(Collectors.toList());
  }

  public Collection<MutationDetails> getUnfinishedRuns() {
    return this.mutationMap.entrySet().stream()
            .filter(hasStatus(DetectionStatus.STARTED))
            .map(toMutationDetails())
            .collect(Collectors.toList());
  }

  public Collection<MutationDetails> getCrashed() {
    // Todo: remove stream style
    return (Collection<MutationDetails>) this.mutationMap.entrySet().stream()
            .filter(or2(hasStatus(DetectionStatus.RUN_ERROR),
                    or2(hasStatus(DetectionStatus.MEMORY_ERROR),
                            hasStatus(DetectionStatus.TIMED_OUT))))
            .map(toMutationDetails())
            .collect(Collectors.toList());
  }

  public Set<MutationDetails> allMutations() {
    return this.mutationMap.keySet();
  }

  private static Function<Entry<MutationDetails, MutationStatusTestPair>, MutationResult> detailsToMutationResults() {
    return a -> new MutationResult(a.getKey(), a.getValue());
  }

  private static Function<Entry<MutationDetails, MutationStatusTestPair>, MutationDetails> toMutationDetails() {
    return a -> a.getKey();
  }

  private static Predicate<Entry<MutationDetails, MutationStatusTestPair>> hasStatus(
          final DetectionStatus status) {
    return a -> a.getValue().getStatus().equals(status);
  }

  public void markUncoveredMutations() {
    setStatusForMutations(
        FCollection.filter(this.mutationMap.keySet(), hasNoCoverage()),
        DetectionStatus.NO_COVERAGE);

  }

  private static Predicate<MutationDetails> hasNoCoverage() {
    return a -> a.getTestsInOrder().isEmpty();
  }

}
