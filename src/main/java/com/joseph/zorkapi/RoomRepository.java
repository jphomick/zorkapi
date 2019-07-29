package com.joseph.zorkapi;

import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;

public interface RoomRepository extends CrudRepository<Room, Long> {
    Room findByXAndY(long x, long y);
    Room findByName(String name);
    ArrayList<Room> findAll();
    ArrayList<Room> findAllByVisited(boolean visited);
}
