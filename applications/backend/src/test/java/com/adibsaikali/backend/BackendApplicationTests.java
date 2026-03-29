/*
 * Copyright 2026 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */

package com.adibsaikali.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestDatabaseConfig.class)
class BackendApplicationTests {

    @Autowired
    private QuoteRepository quoteRepository;

    @Test
    @DisplayName("Database has 5 quotes")
    void databaseHasFiveQuotes() {
        assertThat(quoteRepository.findAll()).hasSize(5);
    }
}
