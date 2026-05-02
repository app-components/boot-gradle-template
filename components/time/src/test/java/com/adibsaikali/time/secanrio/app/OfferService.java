/*
 * Copyright 2024-present the original author or authors.
 */
package com.adibsaikali.time.secanrio.app;

import com.adibsaikali.time.TimeService;

public record OfferService(TimeService timeService) {

  public SpecialOffer createOffer(String customerId) {
    return new SpecialOffer(customerId.length() / 2, timeService.dateTime().plusDays(10));
  }
}
