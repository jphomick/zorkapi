package com.joseph.zorkapi;

import org.springframework.lang.NonNull;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Active {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NonNull
    private long thingId;

    @NonNull
    private int value;

    @NonNull
    private int conquer;

    @NonNull
    private long roomId;

    @NonNull
    private long blockId;

    @NonNull
    private long invId;

    @NonNull
    private String status;

    public Active(long thingId, int value, int conquer, long roomId, long blockId, long invId, String status) {
        this.thingId = thingId;
        this.value = value;
        this.conquer = conquer;
        this.roomId = roomId;
        this.blockId = blockId;
        this.invId = invId;
        this.status = status;
    }

    public Active() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getThingId() {
        return thingId;
    }

    public void setThingId(long thingId) {
        this.thingId = thingId;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getConquer() {
        return conquer;
    }

    public void setConquer(int conquer) {
        this.conquer = conquer;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public long getBlockId() {
        return blockId;
    }

    public void setBlockId(long blockId) {
        this.blockId = blockId;
    }

    public long getInvId() {
        return invId;
    }

    public void setInvId(long invId) {
        this.invId = invId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
