/*
 * Copyright 2026 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */

package com.adibsaikali.backend;

import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Testcontainers configuration shared by integration-style tests.
 *
 * <p>Containers are declared as Spring beans so Spring controls their lifecycle
 * and exposes connection details to the application context.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18");
    }

    @Bean
    @ServiceConnection
    MailpitContainer mailpitContainer() {
        return new MailpitContainer();
    }
}
