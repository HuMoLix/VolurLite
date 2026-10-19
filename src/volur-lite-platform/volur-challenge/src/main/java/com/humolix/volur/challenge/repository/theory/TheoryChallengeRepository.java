package com.humolix.volur.challenge.repository.theory;

import com.humolix.volur.challenge.domain.entity.theory.TheoryChallenge;
import com.humolix.volur.challenge.domain.enums.TheoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TheoryChallengeRepository extends JpaRepository<TheoryChallenge, Long>, JpaSpecificationExecutor<TheoryChallenge> {

    @Override
    @EntityGraph(attributePaths = {"author", "bank"})
    Page<TheoryChallenge> findAll(Specification<TheoryChallenge> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author", "bank"})
    Optional<TheoryChallenge> findById(Long id);

    @Query("SELECT c.bank.id, COUNT(c) FROM TheoryChallenge c WHERE c.bank.id IN :bankIds GROUP BY c.bank.id")
    List<Object[]> countQuestionsByBankIds(@Param("bankIds") List<Long> bankIds);

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
