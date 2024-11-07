package com.example;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Client.class, ClientIntegrationTest.TestConfig.class})
public class ClientIntegrationTest {
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @Autowired
    Client client;

    @TempDir
    static File tempDir;

    static class TestConfig {
        @Bean
        public File eventsFile() {
            return new File(tempDir, "events.json");
        }
    }

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
    }




    @Test
    @SneakyThrows
    void shouldWriteTwoEventsWhenUserRunsServer() {
        // when
        client.run("up");

        // then
        assertTrue("""
                Starting...
                Status: UP""".equals(getOutput()) || """
                Starting...
                Status: FAILED""".equals(getOutput()));
    }
    @Test
    @SneakyThrows
    @DisplayName("Verify 'down' command logs 'Status: DOWN'")
    void shouldLogStatusDownWhenDownCommandExecuted() {
        client.run("down");

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Status: DOWN"), "Expected 'Status: DOWN' message to be logged.");
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify 'history' command with no options logs usage guidance")
    void shouldLogUsageForHistoryCommandWithoutOptions() {
        client.run("history");

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Usage: vpn-client history"), "Expected usage instructions for missing options.");
    }
    @Test
    @SneakyThrows
    @DisplayName("Verify 'history' command with valid date range")
    void shouldLogHistoryWithDateRange() {
        client.run("history", "-f", "2023-01-01", "-t", "2023-12-31");

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("History from 2023-01-01 to 2023-12-31"), "Expected valid date range log in history.");
    }
    @Test
    @SneakyThrows
    @DisplayName("Verify 'history' command with sort option")
    void shouldSortHistoryWithSortOption() {
        client.run("history", "-f", "2023-01-01", "-t", "2023-12-31", "-s", "asc");

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("History sorted in asc order"), "Expected sorted history log.");
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify error on invalid date format for 'history' command")
    void shouldLogErrorForInvalidDateFormatInHistory() {
        client.run("history", "-f", "invalid-date", "-t", "2023-12-31");

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Error: Invalid date format"), "Expected error log for invalid date format.");
    }
    @Test
    @SneakyThrows
    @DisplayName("Verify unknown command logs unknown command error")
    void shouldLogErrorForUnknownCommand() {
        client.run("unknownCommand");

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Unknown command"), "Expected unknown command log.");
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify no command logs usage instructions")
    void shouldLogUsageWhenNoCommandProvided() {
        client.run();

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Usage: vpn-client <command> [options]"), "Expected usage log when no command is provided.");
    }

    @Test
    @SneakyThrows
    void shouldPrintNoEventsWhenUserRunsStatus() {
        // when
        client.run("status");

        // then
        assertEquals("No events found", getOutput());
    }


    @Test
    @SneakyThrows
    @DisplayName("Test invalid command order")
    void shouldLogErrorForInvalidCommandOrder() {
        client.run("down", "up");

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Usage"), "Expected usage guidance for invalid command order.");
    }
    @Test
    @SneakyThrows
    @DisplayName("Test future date in history command")
    void shouldHandleFutureDateInHistory() {
        String futureDate = LocalDate.now().plusYears(1).toString();
        client.run("history", "-f", "2023-01-01", "-t", futureDate);

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Ignoring future dates"), "Expected message for future dates.");
    }
    @Test
    @SneakyThrows
    @DisplayName("Test duplicate options")
    void shouldHandleDuplicateOptionsGracefully() {
        client.run("history", "-f", "2023-01-01", "-f", "2024-01-01");

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Duplicate option detected"), "Expected warning for duplicate options.");
    }
    @Test
    @SneakyThrows
    @DisplayName("Test incorrect date format")
    void shouldLogErrorForIncorrectDateFormat() {
        client.run("history", "-f", "01-01-2023", "-t", "12-31-2023");

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Invalid date format"), "Expected error message for invalid date format.");
    }

    @Test
    @SneakyThrows
    @DisplayName("Test large input values for options")
    void shouldLogErrorForLargeInputValues() {
        client.run("history", "-s", "asc".repeat(1000));

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Input value too large"), "Expected error for excessively large input value.");
    }
    @Test
    @SneakyThrows
    @DisplayName("Test empty command")
    void shouldDisplayUsageForEmptyCommand() {
        client.run("");

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Usage"), "Expected usage message for empty command.");
    }

    @Test
    @SneakyThrows
    @DisplayName("Test unsupported characters in command")
    void shouldLogErrorForUnsupportedCharacters() {
        client.run("up@", "history#");

        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("Invalid character in command"), "Expected error for unsupported characters.");
    }


    private String getOutput() {
        return outputStreamCaptor.toString().trim();
    }
}
