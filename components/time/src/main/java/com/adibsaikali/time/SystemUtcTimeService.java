/*
 * Copyright 2024 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */

package com.adibsaikali.time;

import java.time.Clock;

/** TimeService based on the Clock.systemUTC() clock */
public final class SystemUtcTimeService extends TimeService {

  public SystemUtcTimeService() {
    super(Clock.systemUTC());
  }
}
