package com.zorvyn.finance.repository;

import com.zorvyn.finance.domain.entity.Role;
import com.zorvyn.finance.domain.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);
}
