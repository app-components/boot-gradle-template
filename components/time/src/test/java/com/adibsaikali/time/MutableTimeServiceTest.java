/*
 * Copyright 2023 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */

package com.adibsaikali.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test ability to manipulate clock")
class MutableTimeServiceTest {

  @Test
  @DisplayName("Test moving clock forward")
  void testMovingClockForward() {

    var timeService = new MutableTimeService();
    var clock = timeService.clock;

    var inTwoHours = timeService.instant().plus(Duration.ofHours(2));

    assertThat(clock.instant()).isBefore(inTwoHours);
    timeService.advanceClock(Duration.ofHours(5));
    assertThat(clock.instant()).isAfter(inTwoHours);
  }

  @Test
  @DisplayName("Test moving clock backwards")
  void testMovingClockBackward() {
    var timeService = new MutableTimeService();
    var clock = timeService.clock;

    var twoHoursAgo = timeService.instant().minus(Duration.ofHours(2));

    assertThat(clock.instant()).isAfter(twoHoursAgo);
    timeService.rewindClock(Duration.ofHours(5));
    assertThat(clock.instant()).isBefore(twoHoursAgo);
  }
}
