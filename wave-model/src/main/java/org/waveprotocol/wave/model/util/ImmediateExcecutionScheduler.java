// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

/**
 * A class that does not do scheduling, and calls back immediately.
 *
 * @author zdwang@google.com (David Wang)
 */
public class ImmediateExcecutionScheduler implements Scheduler {

  public void reset() {
    // Do nothing.
  }

  public void schedule(Command task) {
    task.execute();
  }

}
