package com.humolix.volur.challenge.repository.stage.ctf;

import com.humolix.volur.challenge.domain.entity.stage.ctf.CtfChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CtfChallengeRepository extends JpaRepository<CtfChallenge, Long>, JpaSpecificationExecutor<CtfChallenge> {

    Optional<CtfChallenge> findByName(String name);

    boolean existsByName(String name);

    boolean existsByCategoryId(Long categoryId);

}
