package com.sergart.booking_manager_qa.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(Instant timestamp, int status, List<String> errors) {
}