/*
 * Copyright 2024 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */
package com.adibsaikali.time;

import java.time.*;
import java.util.Objects;

/**
 * TimeService defines methods to correctly work with time in way that makes testing time dependant
 * business logic easy. TimeService has a configurable java.time.Clock that it uses for all its time
 * operations.
 *
 * <p>Java has a rich domain for working with time. It is a best practice to always read the current
 * time from a clock rather than the pre `java.time` classes and packages in order to make testing
 * easier. The TimeService wraps a clock and offers convenience methods to work with time.
 */
public abstract class TimeService {
  protected final Clock clock;

  protected TimeService(Clock clock) {
    Objects.requireNonNull(clock);
    this.clock = clock;
  }

  /**
   * @return The clock used by the TimeService
   */
  public Clock clock() {
    return this.clock;
  }

  /**
   * @return the current Instant according to the TimeService clock
   */
  public final Instant instant() {
    return clock.instant();
  }

  /**
   * @return the current date according to the TimeService clock
   */
  public final LocalDate date() {
    return LocalDate.now(this.clock);
  }

  /**
   * @return the current time according to the TimeService clock
   */
  public final LocalTime time() {
    return LocalTime.now(this.clock);
  }

  /**
   * @return the current date & time according to the TimeService clock
   */
  public final LocalDateTime dateTime() {
    return LocalDateTime.now(this.clock);
  }
}
