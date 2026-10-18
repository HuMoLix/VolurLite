package com.humolix.volur.challenge.repository.ctf;

import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CtfChallengeRepository extends JpaRepository<CtfChallenge, Long>, JpaSpecificationExecutor<CtfChallenge> {

    @Override
    @EntityGraph(attributePaths = {"author", "category"})
    Page<CtfChallenge> findAll(Specification<CtfChallenge> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author", "category"})
    Optional<CtfChallenge> findById(Long id);

    Optional<CtfChallenge> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByCategoryId(Long categoryId);

}
