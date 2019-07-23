package com.joseph.zorkapi;

import org.springframework.data.repository.CrudRepository;

public interface PassageRepository extends CrudRepository<Passage, Long> {
    Passage findByRoomFromAndRoomTo(long from, long to);
}
