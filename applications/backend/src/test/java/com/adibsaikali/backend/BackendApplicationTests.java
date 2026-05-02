/*
 * Copyright 2026 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */
package com.adibsaikali.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(TestContainersConfig.class)
class BackendApplicationTests {

  @Autowired private QuoteRepository quoteRepository;

  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setUpMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  @DisplayName("Database has 5 quotes")
  void databaseHasFiveQuotes() {
    assertThat(quoteRepository.findAll()).hasSize(5);
  }

  @Test
  @DisplayName("Random quote API returns a quote")
  void randomQuoteApiReturnsAQuote() throws Exception {
    mockMvc
        .perform(get("/api/quotes/random"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quote").isNotEmpty())
        .andExpect(jsonPath("$.author").isNotEmpty());
  }
}
