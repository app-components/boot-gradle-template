/*
 * Copyright 2026 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */

package com.adibsaikali.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(TestContainersConfig.class)
class QuoteEmailFeatureTests {

    @Autowired
    private MailpitClient mailpitClient;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    @DisplayName("Email quote API sends a message through Mailpit")
    void emailQuoteApiSendsAMessageThroughMailpit() throws Exception {
        MockMvc mockMvc =
                MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        var result = mockMvc.perform(post("/api/quotes/email-random")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reader@example.com"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.recipient").value("reader@example.com"))
                .andExpect(jsonPath("$.quote").isNotEmpty())
                .andExpect(jsonPath("$.author").isNotEmpty())
                .andReturn();

        awaitMessageCount(1);

        String responseBody = result.getResponse().getContentAsString();
        String quote = JsonPath.read(responseBody, "$.quote");
        String author = JsonPath.read(responseBody, "$.author");

        var messages = mailpitClient.getAllMessages();
        assertThat(messages).hasSize(1);

        var message = messages.getFirst();
        assertThat(message.subject()).isEqualTo("Your motivational quote");
        assertThat(message.to()).extracting(address -> address.address()).contains("reader@example.com");
        assertThat(mailpitClient.getMessagePlain(message.id()))
                .contains("Here is your motivational quote:")
                .contains(quote)
                .contains(author);
    }

    private void awaitMessageCount(int expectedCount) throws InterruptedException {
        long timeoutAt = System.nanoTime() + Duration.ofSeconds(5).toNanos();

        while (System.nanoTime() < timeoutAt) {
            if (mailpitClient.getMessageCount() >= expectedCount) {
                return;
            }
            Thread.sleep(100);
        }

        assertThat(mailpitClient.getMessageCount()).isGreaterThanOrEqualTo(expectedCount);
    }
}
