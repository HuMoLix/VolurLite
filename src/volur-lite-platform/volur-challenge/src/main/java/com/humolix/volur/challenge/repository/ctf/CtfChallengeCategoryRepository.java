package com.humolix.volur.challenge.repository.ctf;

import com.humolix.volur.challenge.domain.entity.ctf.CtfChallengeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CtfChallengeCategoryRepository extends JpaRepository<CtfChallengeCategory, Long> {

    Optional<CtfChallengeCategory> findByName(String name);

    Optional<CtfChallengeCategory> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

}
