package org.example.repository;

import java.util.Optional;
import org.example.entity.Role;
import org.example.entity.role.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(RoleName roleName);
}
