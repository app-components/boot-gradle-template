/*
 * Copyright 2024 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */

package com.adibsaikali.time.secanrio.app;

import com.adibsaikali.time.TimeService;

public record CheckoutService(TimeService timeService) {
  public boolean validOffer(SpecialOffer offer) {
    return timeService.dateTime().isBefore(offer.expiryDate());
  }
}
