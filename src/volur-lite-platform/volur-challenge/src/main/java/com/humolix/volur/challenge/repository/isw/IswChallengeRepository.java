package com.humolix.volur.challenge.repository.isw;

import com.humolix.volur.challenge.domain.entity.isw.IswChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IswChallengeRepository extends JpaRepository<IswChallenge, Long>, JpaSpecificationExecutor<IswChallenge> {

    List<IswChallenge> findByCategory(String category);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

}
