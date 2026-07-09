package com.corporate.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.corporate.entity.AgentChatMetric;

public interface AgentChatMetricRepository extends JpaRepository<AgentChatMetric, Long> {
    Optional<AgentChatMetric> findByDraftToken(String draftToken);
}
