package org.example.repository;

import org.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Integer countAllByEmailIn(Iterable<String> emails);

    Optional<User> findByEmail(String email);

    List<User> findAllByEmailIn(Iterable<String> emails);

    boolean existsByEmail(String email);
}
