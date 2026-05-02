/*
 * Copyright 2024-present the original author or authors.
 */
package com.adibsaikali.time;

import java.time.Clock;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("System UTC TimeService tests")
public class SystemUtcTimeServiceTest {

  @Test
  @DisplayName("Works")
  void test() {
    TimeService timeService = new SystemUtcTimeService();

    Assertions.assertThat(timeService.clock()).isEqualTo(Clock.systemUTC());
    Assertions.assertThat(timeService.instant()).isNotNull();
    Assertions.assertThat(timeService.time()).isNotNull();
    Assertions.assertThat(timeService.date()).isNotNull();
    Assertions.assertThat(timeService.dateTime()).isNotNull();
  }
}
