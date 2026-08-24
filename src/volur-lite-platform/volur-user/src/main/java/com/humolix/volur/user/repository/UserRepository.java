package com.humolix.volur.user.repository;

import com.humolix.volur.user.entity.User;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :loginName OR u.email = :loginName")
    Optional<User> findByUsernameOrEmail(@Param("loginName") String loginName);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT count(u) + 1 FROM User u WHERE u.totalPoints > (SELECT u2.totalPoints FROM User u2 WHERE u2.id = :userId)")
    Integer findRankByUserId(@Param("userId") Long userId);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.userProfile up " +
            "WHERE u.id = :id")
    Optional<User> findDetailById(@Param("id") Long id);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.userProfile up " +
            "WHERE u.status = true AND u.isHidden = false " +
            "ORDER BY u.totalPoints DESC")
    Page<User> findGlobalRanking(Pageable pageable);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.userProfile up " +
            "WHERE (u.username LIKE %:keyword% OR up.org LIKE %:keyword%) AND u.status = true AND u.isHidden = false " +
            "ORDER BY u.totalPoints DESC")
    Page<User> findGlobalRankingWithKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userProfile LEFT JOIN FETCH u.role WHERE u.id = :id")
    Optional<User> findByIdWithProfileAndRole(@Param("id") Long id);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.userProfile p " +
            "WHERE u.username = :username")
    Optional<User> findByUsernameWithProfile(@Param("username") String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    Boolean existsByPhone(@Size(max = 20, message = "Phone cannot exceed 20 characters") String phone);

    Boolean existsByRole_Name(String roleName);

}
