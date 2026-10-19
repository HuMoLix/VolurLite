package com.humolix.volur.user.repository;

import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.domain.enums.UserRole;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Query("SELECT u FROM User u WHERE u.username = :loginName OR u.email = :loginName")
    Optional<User> findByUsernameOrEmail(@Param("loginName") String loginName);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(@Size(max = 20, message = "Phone cannot exceed 20 characters") String phone);

    boolean existsByRole(UserRole role);

    List<User> findByRoleAndUsernameNot(UserRole role, String username);

    long countByRoleIn(List<UserRole> roles);

    long countByStatus(Boolean status);

}
