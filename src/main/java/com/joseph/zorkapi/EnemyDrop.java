package com.joseph.zorkapi;

import org.springframework.lang.NonNull;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class EnemyDrop {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NonNull
    private long specificActive;

    @NonNull
    private String generalThing;

    @NonNull
    private int type;

    @NonNull
    private int chance;

    @NonNull
    private String item;

    public EnemyDrop(long specificActive, String generalThing, int type, String item, int chance) {
        this.specificActive = specificActive;
        this.generalThing = generalThing;
        this.type = type;
        this.item = item;
        this.chance = chance;
    }

    public EnemyDrop(long specificActive, String item, int chance) {
        this.specificActive = specificActive;
        this.item = item;
        this.type = 1;
        this.generalThing = "";
        this.chance = chance;
    }

    public EnemyDrop(String generalThing, String item, int chance) {
        this.generalThing = generalThing;
        this.item = item;
        this.type = 0;
        this.specificActive = -1;
        this.chance = chance;
    }

    public EnemyDrop() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSpecificActive() {
        return specificActive;
    }

    public void setSpecificActive(long specificActive) {
        this.specificActive = specificActive;
    }

    public String getGeneralThing() {
        return generalThing;
    }

    public void setGeneralThing(String generalThing) {
        this.generalThing = generalThing;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }
}
