package com.example;

import java.util.List;

public record Events(List<Event> events) {
}

record Event(Status status, long timestamp) {
}

enum Status {
    STARTING,
    STOPPING,
    UP,
    DOWN,
    FAILED
}
