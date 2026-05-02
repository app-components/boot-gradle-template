/*
 * Copyright 2026-present the original author or authors.
 */
package com.adibsaikali.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuoteRepository extends JpaRepository<Quote, Integer> {

  @Query(
      nativeQuery = true,
      value = "SELECT id, quote, author FROM quotes ORDER BY RANDOM() LIMIT 1")
  Quote findRandomQuote();
}
