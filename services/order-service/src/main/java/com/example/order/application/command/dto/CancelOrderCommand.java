package com.example.order.application.command.dto;

import java.util.UUID;

public record CancelOrderCommand(UUID orderId, String reason, String requestedByUserId) {}
