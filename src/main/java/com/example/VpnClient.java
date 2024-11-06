package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class VpnClient implements CommandLineRunner {
    private static final String STATUS_COMMAND = "status";
    private static final String UP_COMMAND = "up";
    private static final String DOWN_COMMAND = "down";
    private static final String HISTORY_COMMAND = "history";
    private static final String EVENTS_JSON_FILENAME = "events.json";

    private final static Map<String, Options> COMMANDS = Map.of(
            STATUS_COMMAND, new Options(),
            UP_COMMAND, new Options(),
            DOWN_COMMAND, new Options(),
            HISTORY_COMMAND, new Options()
                    .addOption("f", "from", true, "From")
                    .addOption("t", "to", true, "To")
                    .addOption("s", "sort", true, "Sort")
                    .addOption("S", "status", true, "Status")
    );

    public static void main(String[] args) {
        SpringApplication.run(VpnClient.class, args);
    }

    @Override
    public void run(String... args) throws ParseException, IOException {
        CommandLineParser parser = new DefaultParser();

        if (args.length == 0) {
            System.out.println("Usage: vpn-client <command> [options]");
            System.out.println("Commands:");
            COMMANDS.keySet().forEach(command -> System.out.println("  " + command));
            return;
        }

        String command = args[0];

        if (!COMMANDS.containsKey(command)) {
            System.out.println("Unknown command: " + command);
            return;
        }

        Options options = COMMANDS.get(command);
        CommandLine cmd = parser.parse(options, args);

        switch (command) {
            case STATUS_COMMAND -> {
                Event latestNotFailedEvent = getLatestNotFailedEvent();
                System.out.println("Status: " + latestNotFailedEvent.status());

                if (latestNotFailedEvent.status() == Status.UP) {
                    int uptime = getTimestamp() - latestNotFailedEvent.timestamp();
                    System.out.println("Uptime: " + uptime + " seconds");
                }
            }
            case UP_COMMAND -> {
                if (getLatestNotFailedEvent().status() == Status.UP) {
                    System.out.println("Already UP");
                } else {
                    Status randomResult = new Random().nextBoolean() ? Status.UP : Status.FAILED;
                    writeEventToFile(new Event(Status.STARTING, getTimestamp()));
                    writeEventToFile(new Event(randomResult, getTimestamp()));
                }
            }
            case DOWN_COMMAND -> {
                if (getLatestNotFailedEvent().status() == Status.DOWN) {
                    System.out.println("Already DOWN");
                } else {
                    Status randomResult = new Random().nextBoolean() ? Status.DOWN : Status.FAILED;
                    writeEventToFile(new Event(Status.STOPPING, getTimestamp()));
                    writeEventToFile(new Event(randomResult, getTimestamp()));
                }
            }
            case HISTORY_COMMAND -> {
                String from = cmd.getOptionValue("from");
                String to = cmd.getOptionValue("to");
                String sort = cmd.getOptionValue("sort");
                String status = cmd.getOptionValue("status");

                System.out.println(sort + " " + status);

                filterEvents(from, to, sort, status).forEach(event -> {
                    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(event.timestamp(), 0, ZoneOffset.UTC);
                    System.out.println("Status: " + event.status() + ", Timestamp: " + dateTime);
                });
            }
        }
    }

    private Event getLatestNotFailedEvent() throws IOException {
        Event latestEvent = getLatestEvent();

        if (latestEvent.status() == Status.FAILED) {
            return getLatestEventByStatus(Status.UP, Status.DOWN);
        } else {
            return latestEvent;
        }
    }

    private static int getTimestamp() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    private void writeEventToFile(Event event) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File("events.json");

        if (file.createNewFile()) {
            objectMapper.writeValue(file, new Events(Collections.emptyList()));
        }

        Events events = objectMapper.readValue(file, Events.class);
        events.events().add(event);
        objectMapper.writeValue(file, events);
    }

    private Event getLatestEventByStatus(Status... status) throws IOException {
        return readAllEvents().events().stream()
                .filter(event -> {
                    for (Status s : status) {
                        if (event.status() == s) {
                            return true;
                        }
                    }
                    return false;
                })
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private Event getLatestEvent() throws IOException {
        return readAllEvents().events().stream()
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private List<Event> filterEvents(String from, String to, String sort, String status) throws IOException {
        List<Event> events = readAllEvents().events();

        return events.stream()
                .filter(event -> {
                    if (from != null && event.timestamp() < Integer.parseInt(from)) {
                        return false;
                    }
                    if (to != null && event.timestamp() > Integer.parseInt(to)) {
                        return false;
                    }
                    return status == null || event.status().equals(Status.valueOf(status));
                })
                .sorted((event1, event2) -> {
                    if (sort != null) {
                        if (sort.equals("asc")) {
                            return Integer.compare(event1.timestamp(), event2.timestamp());
                        } else if (sort.equals("desc")) {
                            return Integer.compare(event2.timestamp(), event1.timestamp());
                        }
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    private Events readAllEvents() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(EVENTS_JSON_FILENAME);

        if (file.createNewFile()) {
            objectMapper.writeValue(file, new Events(Collections.emptyList()));
        }

        return objectMapper.readValue(file, Events.class);
    }
}