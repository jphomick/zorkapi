package com.joseph.zorkapi;

import org.springframework.data.repository.CrudRepository;

public interface RoomRepository extends CrudRepository<Room, Long> {
    Room findByXAndY(long x, long y);
}
