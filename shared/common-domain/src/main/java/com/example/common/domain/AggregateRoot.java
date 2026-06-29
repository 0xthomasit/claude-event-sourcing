package com.example.common.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for aggregate roots supporting domain event tracking and versioning.
 */
public abstract class AggregateRoot {

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    protected long version;

    protected void raiseEvent(DomainEvent event) {
        if (event != null) {
            this.uncommittedEvents.add(event);
        }
    }

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void clearUncommittedEvents() {
        this.uncommittedEvents.clear();
    }

    public long getVersion() {
        return version;
    }

    protected void incrementVersion() {
        this.version++;
    }
}
