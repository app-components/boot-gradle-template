/*
 * Copyright 2024-present the original author or authors.
 */
package com.adibsaikali.time;

import java.time.Clock;

/** TimeService based on the Clock.systemUTC() clock */
public final class SystemUtcTimeService extends TimeService {

  public SystemUtcTimeService() {
    super(Clock.systemUTC());
  }
}
