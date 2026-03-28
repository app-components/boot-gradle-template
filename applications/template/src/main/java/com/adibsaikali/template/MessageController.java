/*
 * Copyright 2026 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */

package com.adibsaikali.template;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MessageController {

    private final QuoteRepository quoteRepository;

    public MessageController(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    @GetMapping("/")
    public Quote randomQuote() {
        return quoteRepository.findRandomQuote();
    }

    @GetMapping("/quotes")
    public List<Quote> getAll() {
        return quoteRepository.findAll();
    }

    @GetMapping("/quotes/{id}")
    public Quote getQuote(@PathVariable Integer id) {
        return quoteRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
