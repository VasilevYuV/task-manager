    package com.vailevyuv.springproject.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ValidationErrorDto(
    LocalDateTime timestamp,
    int status,
    String message,
    List<FieldErrorDto> errors
) {}