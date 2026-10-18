package com.humolix.volur.competition.repository.theory;

import com.humolix.volur.competition.domain.entity.theory.TheoryCompetitionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheoryCompetitionBankRepository extends JpaRepository<TheoryCompetitionBank, Long> {

    @Query("SELECT b FROM TheoryCompetitionBank b JOIN FETCH b.bank WHERE b.competition.id = :competitionId")
    List<TheoryCompetitionBank> findByCompetitionId(@Param("competitionId") Long competitionId);

    void deleteByCompetitionId(Long competitionId);

}
