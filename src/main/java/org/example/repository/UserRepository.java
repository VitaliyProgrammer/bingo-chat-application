package org.example.repository;

import java.util.List;
import java.util.Optional;
import org.example.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickName);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    List<User> findByNicknameContainingIgnoreCase(String nickName);
}
