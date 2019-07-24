package com.joseph.zorkapi;

import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;

public interface ActiveRepository extends CrudRepository<Active, Long> {
    ArrayList<Active> findAllByRoomId(long id);
}
