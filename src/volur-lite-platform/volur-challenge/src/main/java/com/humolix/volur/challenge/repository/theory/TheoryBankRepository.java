package com.humolix.volur.challenge.repository.theory;

import com.humolix.volur.challenge.domain.entity.theory.TheoryBank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TheoryBankRepository extends JpaRepository<TheoryBank, Long>, JpaSpecificationExecutor<TheoryBank> {

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<TheoryBank> findAll(Specification<TheoryBank> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author"})
    Optional<TheoryBank> findById(Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<TheoryBank> findByName(String name);

}
