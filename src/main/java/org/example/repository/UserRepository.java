package org.example.repository;

import java.util.List;
import java.util.Optional;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickName(String nickName);

    Optional<User> findByEmail(String email);

    List<User> findByNickNameContainingIgnoreCase(String nickName);
}
