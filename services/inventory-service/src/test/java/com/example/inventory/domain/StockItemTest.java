package com.example.inventory.domain;

import com.example.common.events.inventory.*;
import com.example.inventory.domain.model.StockItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StockItem Aggregate")
class StockItemTest {

    @Test
    @DisplayName("initialize() — creates StockItem with initial quantity")
    void initialize_createsWithInitialQty() {
        StockItem item = StockItem.initialize("prod-001", "WH-01", 100);

        assertThat(item.getProductId()).isEqualTo("prod-001");
        assertThat(item.getAvailableQuantity()).isEqualTo(100);
        assertThat(item.getReservedQuantity()).isEqualTo(0);
        assertThat(item.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(StockReplenishedEvent.class);
    }

    @Test
    @DisplayName("reserve() — reduces available, increases reserved")
    void reserve_reducesAvailableIncreasesReserved() {
        StockItem item = StockItem.initialize("prod-001", "WH-01", 100);
        item.clearUncommittedEvents();

        item.reserve("order-001", 30);

        assertThat(item.getAvailableQuantity()).isEqualTo(70);
        assertThat(item.getReservedQuantity()).isEqualTo(30);
        assertThat(item.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(StockReservedEvent.class);
    }

    @Test
    @DisplayName("reserve() — insufficient stock throws IllegalStateException")
    void reserve_insufficientStock_throws() {
        StockItem item = StockItem.initialize("prod-001", "WH-01", 10);

        assertThatIllegalStateException()
                .isThrownBy(() -> item.reserve("order-001", 50))
                .withMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("release() — restores available from reserved")
    void release_restoresAvailable() {
        StockItem item = StockItem.initialize("prod-001", "WH-01", 100);
        item.reserve("order-001", 30);
        item.clearUncommittedEvents();

        item.release("order-001", 30);

        assertThat(item.getAvailableQuantity()).isEqualTo(100);
        assertThat(item.getReservedQuantity()).isEqualTo(0);
        assertThat(item.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(StockReleasedEvent.class);
    }

    @Test
    @DisplayName("reduce() — permanently deducts from reserved")
    void reduce_deductsFromReserved() {
        StockItem item = StockItem.initialize("prod-001", "WH-01", 100);
        item.reserve("order-001", 30);
        item.clearUncommittedEvents();

        item.reduce("order-001", 30);

        assertThat(item.getReservedQuantity()).isEqualTo(0);
        assertThat(item.getTotalQuantity()).isEqualTo(70);
        assertThat(item.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(StockReducedEvent.class);
    }

    @Test
    @DisplayName("adjust() — positive delta increases available")
    void adjust_positiveDelta_increasesAvailable() {
        StockItem item = StockItem.initialize("prod-001", "WH-01", 50);
        item.clearUncommittedEvents();

        item.adjust(20, "STOCKTAKE");

        assertThat(item.getAvailableQuantity()).isEqualTo(70);
        assertThat(item.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(StockAdjustedEvent.class);
    }

    @Test
    @DisplayName("adjust() — negative delta that causes negative stock throws")
    void adjust_negativeDeltaBelowZero_throws() {
        StockItem item = StockItem.initialize("prod-001", "WH-01", 10);

        assertThatIllegalStateException()
                .isThrownBy(() -> item.adjust(-20, "DAMAGE"))
                .withMessageContaining("negative stock");
    }

    @Test
    @DisplayName("version increments with each event applied")
    void version_incrementsWithEachEvent() {
        StockItem item = StockItem.initialize("prod-001", "WH-01", 100);
        assertThat(item.getVersion()).isEqualTo(1);

        item.reserve("order-001", 10);
        assertThat(item.getVersion()).isEqualTo(2);

        item.release("order-001", 10);
        assertThat(item.getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("hasEnoughStock() — returns correct availability")
    void hasEnoughStock_returnsCorrectly() {
        StockItem item = StockItem.initialize("prod-001", "WH-01", 50);

        assertThat(item.hasEnoughStock(50)).isTrue();
        assertThat(item.hasEnoughStock(51)).isFalse();

        item.reserve("order-001", 20);
        assertThat(item.hasEnoughStock(30)).isTrue();
        assertThat(item.hasEnoughStock(31)).isFalse();
    }
}