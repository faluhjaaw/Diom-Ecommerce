package com.dic1.projettrans.authentication.repositories;

import com.dic1.projettrans.authentication.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User,String> {
    Optional<User> findByEmail(String email);
}
