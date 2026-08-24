package com.humolix.volur.challenge.repository.stage.ctf;

import com.humolix.volur.challenge.entity.stage.ctf.CtfChallenge;
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
    @EntityGraph(attributePaths = {"category"})
    Optional<CtfChallenge> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "author"})
    Page<CtfChallenge> findAll(Specification<CtfChallenge> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Optional<CtfChallenge> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"category"})
    Optional<CtfChallenge> findByIdAndDeletedTrue(Long id);

    @EntityGraph(attributePaths = {"category", "author"})
    Page<CtfChallenge> findByDeletedTrue(Pageable pageable);

    boolean existsByIdAndDeletedFalse(Long id);

    Long countByCategoryId(Long categoryId);

}
