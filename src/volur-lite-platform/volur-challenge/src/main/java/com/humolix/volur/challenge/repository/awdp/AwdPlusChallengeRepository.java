package com.humolix.volur.challenge.repository.awdp;

import com.humolix.volur.challenge.domain.entity.awdp.AwdPlusChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwdPlusChallengeRepository extends JpaRepository<AwdPlusChallenge, Long>, JpaSpecificationExecutor<AwdPlusChallenge> {

    List<AwdPlusChallenge> findByCategory(String category);

    boolean existsByName(String name);

}
