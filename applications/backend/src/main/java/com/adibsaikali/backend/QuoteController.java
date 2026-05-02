/*
 * Copyright 2026 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */
package com.adibsaikali.backend;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class QuoteController {

  private final QuoteRepository quoteRepository;
  private final QuoteEmailService quoteEmailService;

  public QuoteController(QuoteRepository quoteRepository, QuoteEmailService quoteEmailService) {
    this.quoteRepository = quoteRepository;
    this.quoteEmailService = quoteEmailService;
  }

  @GetMapping("/api/quotes/random")
  public Quote randomQuote() {
    return quoteRepository.findRandomQuote();
  }

  @GetMapping("/api/quotes")
  public List<Quote> getAll() {
    return quoteRepository.findAll();
  }

  @GetMapping("/api/quotes/{id}")
  public Quote getQuote(@PathVariable Integer id) {
    return quoteRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping("/api/quotes/email-random")
  public ResponseEntity<QuoteEmailResponse> emailRandomQuote(
      @RequestBody QuoteEmailRequest request) {
    return ResponseEntity.accepted().body(quoteEmailService.emailRandomQuote(request.email()));
  }
}
