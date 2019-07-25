package com.joseph.zorkapi;

import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;

public interface ActiveRepository extends CrudRepository<Active, Long> {
    ArrayList<Active> findAllByRoomId(long id);
    ArrayList<Active> findAllByRoomIdAndThingId(long roomId, long thingId);
    ArrayList<Active> findAllByInvId(long id);
    ArrayList<Active> findAllByInvIdAndThingId(long invId, long thingId);
    //Active findByInvIdAndThingId(long invId, long thingId);
    Active findByValue(int value);
    ArrayList<Active> findAllByBlockId(long id);
}
