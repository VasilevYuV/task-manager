package com.vailevyuv.springproject.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Task(
    UUID id,
    String title,
    String description,
    TaskStatus status,
    LocalDateTime createdAt,
    LocalDateTime dueDate
) {}