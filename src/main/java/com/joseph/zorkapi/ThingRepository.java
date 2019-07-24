package com.joseph.zorkapi;

import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;

public interface ThingRepository extends CrudRepository<Thing, Long> {
    ArrayList<Thing> findAllByType(String type);
}
