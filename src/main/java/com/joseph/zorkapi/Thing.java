package com.joseph.zorkapi;

import org.springframework.lang.NonNull;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Thing {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NonNull
    private String name;

    @NonNull
    private int value;

    @NonNull
    private int value2;

    @NonNull
    private String code;

    @NonNull
    private int conquer;

    @NonNull
    private int block;

    @NonNull
    private String actions;

    @NonNull
    private String type;

    public Thing(String name, int value, int value2, String code, int conquer, int block, String actions, String type) {
        this.name = name;
        this.value = value;
        this.value2 = value2;
        this.code = code;
        this.conquer = conquer;
        this.block = block;
        this.actions = actions;
        this.type = type;
    }

    public Thing() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getConquer() {
        return conquer;
    }

    public void setConquer(int conquer) {
        this.conquer = conquer;
    }

    public int getBlock() {
        return block;
    }

    public void setBlock(int block) {
        this.block = block;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getValue2() {
        return value2;
    }

    public void setValue2(int value2) {
        this.value2 = value2;
    }
}
