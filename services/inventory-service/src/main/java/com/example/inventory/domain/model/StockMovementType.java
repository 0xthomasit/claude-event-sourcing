package com.example.inventory.domain.model;

public enum StockMovementType {
    RESERVE,       // lock stock for an order
    RELEASE,       // unlock stock when order cancelled
    REDUCE,        // permanently deduct when order shipped
    REPLENISH,     // add stock (purchase order received)
    ADJUST         // manual correction (stocktake, damage)
}
