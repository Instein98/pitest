package org.pitest.mutationtest.execute;

import org.pitest.functional.SideEffect;
import org.pitest.util.ExitCode;

public class TimeOutSystemExitSideEffect implements SideEffect {

  private final Reporter r;

  public Reporter getR() {
    return r;
  }

  public TimeOutSystemExitSideEffect(final Reporter r) {
    this.r = r;
  }

  @Override
  public void apply() {
    this.r.done(ExitCode.TIMEOUT);
  }

}
