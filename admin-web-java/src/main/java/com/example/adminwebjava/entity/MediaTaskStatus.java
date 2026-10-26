package com.example.adminwebjava.entity;

public enum MediaTaskStatus {
    PENDING(0),
    PROCESSING(1),
    COMPLETED(2),
    COMPLETED_WITH_ERRORS(3),
    FAILED(4);

    private final int value;

    MediaTaskStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}