/*
 * Copyright 2026 Programming Mastery Inc.
 *
 * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential
 */

package com.adibsaikali.template.note;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@DisplayName("NoteRepository Postgres tests")
class NoteRepositoryPostgresTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @Autowired
    private NoteRepository noteRepository;

    @Test
    @DisplayName("Saves and loads a note using Postgres")
    void savesAndLoadsNote() {
        var savedNote = noteRepository.save(new Note("template note"));

        assertThat(savedNote.getId()).isNotNull();
        assertThat(noteRepository.findById(savedNote.getId())).hasValueSatisfying(note -> {
            assertThat(note.getTitle()).isEqualTo("template note");
            assertThat(note.getCreatedAt()).isNotNull();
        });
    }
}
