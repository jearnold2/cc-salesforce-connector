package com.salesforce.emp.connector.object;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

public class Event {
    ZonedDateTime createdDate;
    int replayId;
    String type;

    String id;
    String description;
    String name;

    public Event(ZonedDateTime createdDate, int replayId, String type) {
        this.createdDate = createdDate;
        this.replayId = replayId;
        this.type = type;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public int getReplayId() {
        return replayId;
    }

    public void setReplayId(int replayId) {
        this.replayId = replayId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
