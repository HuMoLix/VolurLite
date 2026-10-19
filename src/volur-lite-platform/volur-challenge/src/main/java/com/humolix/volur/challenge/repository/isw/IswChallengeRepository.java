package com.humolix.volur.challenge.repository.isw;

import com.humolix.volur.challenge.domain.entity.isw.IswChallenge;
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
public interface IswChallengeRepository extends JpaRepository<IswChallenge, Long>, JpaSpecificationExecutor<IswChallenge> {

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<IswChallenge> findAll(Specification<IswChallenge> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author"})
    Optional<IswChallenge> findById(Long id);

    List<IswChallenge> findByCategory(String category);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

}
