package com.humolix.volur.challenge.repository.theory;

import com.humolix.volur.challenge.domain.entity.theory.TheoryBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TheoryBankRepository extends JpaRepository<TheoryBank, Long>, JpaSpecificationExecutor<TheoryBank> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<TheoryBank> findByName(String name);

}
