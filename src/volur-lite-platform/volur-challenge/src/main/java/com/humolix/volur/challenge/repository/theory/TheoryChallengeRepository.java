package com.humolix.volur.challenge.repository.theory;

import com.humolix.volur.challenge.domain.entity.theory.TheoryChallenge;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheoryChallengeRepository extends JpaRepository<TheoryChallenge, Long>, JpaSpecificationExecutor<TheoryChallenge> {

    List<TheoryChallenge> findByType(TheoryType type);

    List<TheoryChallenge> findByCategory(String category);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    long countByBankId(Long bankId);

    List<TheoryChallenge> findByBankId(Long bankId);

    List<TheoryChallenge> findByBankIdIn(List<Long> bankIds);

    List<TheoryChallenge> findByBankIdInAndType(List<Long> bankIds, TheoryType type);

    void deleteByBankId(Long bankId);

}
