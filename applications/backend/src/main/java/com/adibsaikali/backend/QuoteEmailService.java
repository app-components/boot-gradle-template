/*
 * Copyright 2026-present the original author or authors.
 */
package com.adibsaikali.backend;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuoteEmailService {

  private final QuoteRepository quoteRepository;
  private final JavaMailSender mailSender;
  private final String fromAddress;

  public QuoteEmailService(
      QuoteRepository quoteRepository,
      JavaMailSender mailSender,
      @Value("${app.mail.from}") String fromAddress) {
    this.quoteRepository = quoteRepository;
    this.mailSender = mailSender;
    this.fromAddress = fromAddress;
  }

  public QuoteEmailResponse emailRandomQuote(String email) {
    String recipient = validateEmail(email);
    Quote quote = quoteRepository.findRandomQuote();
    if (quote == null) {
      throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "No quote available to send");
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(recipient);
    message.setSubject("Your motivational quote");
    message.setText(
        """
                Here is your motivational quote:

                "%s"

                — %s
                """
            .formatted(quote.getQuote(), quote.getAuthor()));
    mailSender.send(message);

    return new QuoteEmailResponse(quote.getId(), quote.getQuote(), quote.getAuthor(), recipient);
  }

  private String validateEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new ResponseStatusException(BAD_REQUEST, "Email address is required");
    }

    try {
      InternetAddress address = new InternetAddress(email);
      address.validate();
      return address.getAddress();
    } catch (AddressException ex) {
      throw new ResponseStatusException(BAD_REQUEST, "Email address is invalid");
    }
  }
}
