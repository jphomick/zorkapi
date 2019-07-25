package com.joseph.zorkapi;

import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;

public interface DropRepository extends CrudRepository<EnemyDrop, Long> {
    ArrayList<EnemyDrop> findAllByGeneralThing(String name);
    ArrayList<EnemyDrop> findAllBySpecificActive(Long id);
}
