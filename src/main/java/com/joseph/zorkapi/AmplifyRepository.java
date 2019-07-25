package com.joseph.zorkapi;

import org.springframework.data.repository.CrudRepository;

public interface AmplifyRepository extends CrudRepository<Amplify, Long> {
    Amplify findByKeyword(String keyword);
}
