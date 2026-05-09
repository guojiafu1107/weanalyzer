package com.weanalyzer.repository;

import com.weanalyzer.entity.AiSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, Long> {

    List<AiSuggestion> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    List<AiSuggestion> findByAccountIdAndSuggestionTypeOrderByCreatedAtDesc(Long accountId, AiSuggestion.SuggestionType type);
}
