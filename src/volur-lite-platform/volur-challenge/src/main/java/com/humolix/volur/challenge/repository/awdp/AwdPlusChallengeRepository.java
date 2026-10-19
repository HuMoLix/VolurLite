package com.humolix.volur.challenge.repository.awdp;

import com.humolix.volur.challenge.domain.entity.awdp.AwdPlusChallenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AwdPlusChallengeRepository extends JpaRepository<AwdPlusChallenge, Long>, JpaSpecificationExecutor<AwdPlusChallenge> {

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<AwdPlusChallenge> findAll(Specification<AwdPlusChallenge> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author"})
    Optional<AwdPlusChallenge> findById(Long id);

    List<AwdPlusChallenge> findByCategory(String category);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

}
