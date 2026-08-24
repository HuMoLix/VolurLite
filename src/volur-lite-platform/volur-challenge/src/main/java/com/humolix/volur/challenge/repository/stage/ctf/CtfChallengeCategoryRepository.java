package com.humolix.volur.challenge.repository.stage.ctf;

import com.humolix.volur.challenge.entity.stage.ctf.CtfChallengeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CtfChallengeCategoryRepository extends JpaRepository<CtfChallengeCategory, Long>, JpaSpecificationExecutor<CtfChallengeCategory> {

    Optional<CtfChallengeCategory> findByName(String name);

}
