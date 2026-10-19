package com.humolix.volur.challenge.repository.awd;

import com.humolix.volur.challenge.domain.entity.awd.AwdChallenge;
import com.humolix.volur.challenge.domain.enums.AwdCategory;
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
public interface AwdChallengeRepository extends JpaRepository<AwdChallenge, Long>, JpaSpecificationExecutor<AwdChallenge> {

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<AwdChallenge> findAll(Specification<AwdChallenge> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author"})
    Optional<AwdChallenge> findById(Long id);

    List<AwdChallenge> findByCategory(AwdCategory category);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

}
