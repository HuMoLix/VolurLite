package com.humolix.volur.challenge.repository.stage.awd;

import com.humolix.volur.challenge.domain.entity.stage.awd.AwdChallenge;
import com.humolix.volur.challenge.domain.enums.AwdCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwdChallengeRepository extends JpaRepository<AwdChallenge, Long>, JpaSpecificationExecutor<AwdChallenge> {

    List<AwdChallenge> findByCategory(AwdCategory category);

    boolean existsByName(String name);

}
