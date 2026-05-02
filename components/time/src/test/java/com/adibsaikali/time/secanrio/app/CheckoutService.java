/*
 * Copyright 2024-present the original author or authors.
 */
package com.adibsaikali.time.secanrio.app;

import com.adibsaikali.time.TimeService;

public record CheckoutService(TimeService timeService) {
  public boolean validOffer(SpecialOffer offer) {
    return timeService.dateTime().isBefore(offer.expiryDate());
  }
}
