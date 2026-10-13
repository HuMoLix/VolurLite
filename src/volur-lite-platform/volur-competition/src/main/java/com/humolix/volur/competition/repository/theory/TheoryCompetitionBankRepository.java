package com.humolix.volur.competition.repository.theory;

import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheoryCompetitionBankRepository extends JpaRepository<TheoryCompetitionBank, Long> {

    List<TheoryCompetitionBank> findByCompetitionId(Long competitionId);

    void deleteByCompetitionId(Long competitionId);

}
