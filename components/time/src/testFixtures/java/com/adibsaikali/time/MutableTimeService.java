/*
 * Copyright 2024 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */
package com.adibsaikali.time;

import java.time.Duration;
import org.threeten.extra.MutableClock;

/**
 * A TimeService that simplifies testing of time dependant business logic by allowing the clock to
 * be moved forward or backwards. For, example consider a checkout service that needs to compute the
 * price of shopping cart, but it needs to check that a discount special offer has not expired. In
 * the test scenario you can advance the clock to be after the expiry of the special offer.
 */
public final class MutableTimeService extends TimeService {

  public MutableTimeService() {
    super(MutableClock.epochUTC());
  }

  /**
   * Advanced the TimeService clock by the specified duration
   *
   * @param duration the amount of time to advance the clock by
   */
  public void advanceClock(Duration duration) {
    ((MutableClock) this.clock).add(duration);
  }

  /**
   * Rewind the TimeService clock by the specified duration
   *
   * @param duration the amount of time to rewind the clock by
   */
  public void rewindClock(Duration duration) {
    ((MutableClock) this.clock).add(duration.negated());
  }
}
