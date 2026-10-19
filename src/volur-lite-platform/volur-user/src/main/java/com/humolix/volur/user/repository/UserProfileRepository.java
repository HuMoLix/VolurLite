package com.humolix.volur.user.repository;

import com.humolix.volur.user.domain.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    @Query("SELECT p.id FROM UserProfile p WHERE LOWER(p.org) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Long> findUserIdsByOrgLike(@Param("keyword") String keyword);

}
