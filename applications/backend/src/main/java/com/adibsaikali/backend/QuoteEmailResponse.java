/*
 * Copyright 2026-present the original author or authors.
 */
package com.adibsaikali.backend;

public record QuoteEmailResponse(Integer quoteId, String quote, String author, String recipient) {}
