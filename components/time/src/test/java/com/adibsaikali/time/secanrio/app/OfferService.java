/*
 * Copyright 2024 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */
package com.adibsaikali.time.secanrio.app;

import com.adibsaikali.time.TimeService;

public record OfferService(TimeService timeService) {

  public SpecialOffer createOffer(String customerId) {
    return new SpecialOffer(customerId.length() / 2, timeService.dateTime().plusDays(10));
  }
}
