package com.joseph.zorkapi;

import org.springframework.lang.NonNull;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Passage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NonNull
    private long roomFrom;

    @NonNull
    private long roomTo;

    @NonNull
    private boolean reversable;

    public Passage(long roomFrom, long roomTo, boolean reversable) {
        this.roomFrom = roomFrom;
        this.roomTo = roomTo;
        this.reversable = reversable;
    }

    public Passage() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRoomFrom() {
        return roomFrom;
    }

    public void setRoomFrom(long roomFrom) {
        this.roomFrom = roomFrom;
    }

    public long getRoomTo() {
        return roomTo;
    }

    public void setRoomTo(long roomTo) {
        this.roomTo = roomTo;
    }

    public boolean isReversable() {
        return reversable;
    }

    public void setReversable(boolean reversable) {
        this.reversable = reversable;
    }
}
