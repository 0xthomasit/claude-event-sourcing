package com.example.order.application.command.dto;

import java.util.UUID;

public record ConfirmOrderCommand(UUID orderId, String requestedByUserId) {}
