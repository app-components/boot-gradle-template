/*
 * Copyright 2026 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */

package com.adibsaikali.template;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Testcontainers configuration shared by integration-style tests.
 *
 * <p>The PostgreSQL container is declared as a Spring bean so Spring controls
 * its lifecycle and exposes the connection through {@link ServiceConnection}.
 * This avoids manual property wiring in individual tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18");
    }
}
