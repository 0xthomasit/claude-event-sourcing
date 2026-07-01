package com.example.order.application.command.dto;

import java.util.UUID;

public record ShipOrderCommand(UUID orderId, String trackingNumber, String requestedByUserId) {}
