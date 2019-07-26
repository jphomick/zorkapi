package com.joseph.zorkapi;

import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;

public interface PassageRepository extends CrudRepository<Passage, Long> {
    Passage findByRoomFromAndRoomTo(long from, long to);
    ArrayList<Passage> findAllByRoomFromOrRoomToAndReversableTrue(long from, long to);
    ArrayList<Passage> findAllByRoomFrom(long from);
}
