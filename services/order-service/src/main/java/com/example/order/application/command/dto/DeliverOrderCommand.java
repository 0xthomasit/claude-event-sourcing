package com.example.order.application.command.dto;

import java.util.UUID;

public record DeliverOrderCommand(UUID orderId, String requestedByUserId) {}
