/*
 * Copyright 2024 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */

package com.adibsaikali.time.secanrio.test;

import com.adibsaikali.time.MutableTimeService;
import com.adibsaikali.time.secanrio.app.CheckoutService;
import com.adibsaikali.time.secanrio.app.OfferService;
import java.time.Duration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Recommended TimeService Usage")
class RecommendedUsageExampleTest {

  @Test
  @DisplayName("Time sensitive business logic is easy to test")
  void testTimeTravel() {

    var timeService = new MutableTimeService();
    var offerService = new OfferService(timeService);
    var checkoutService = new CheckoutService(timeService);

    var offer = offerService.createOffer("123");
    timeService.advanceClock(Duration.ofDays(100));

    Assertions.assertThat(checkoutService.validOffer(offer)).isFalse();
  }
}
