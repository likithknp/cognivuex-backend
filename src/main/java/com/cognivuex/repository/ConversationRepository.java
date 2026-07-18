package com.cognivuex.repository;

import com.cognivuex.entity.Conversation;
import com.cognivuex.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Find the latest conversation for a user and report
     */
    Optional<Conversation> findFirstByUserAndReportOrderByUpdatedAtDesc(User user, Long reportId);

    /**
     * Find all conversations for a user
     */
    List<Conversation> findAllByUserOrderByUpdatedAtDesc(User user);

    /**
     * Find the most recent conversation for a user (regardless of report)
     */
    Optional<Conversation> findFirstByUserOrderByUpdatedAtDesc(User user);

    /**
     * Find conversations for a specific report
     */
    List<Conversation> findAllByReportIdOrderByUpdatedAtDesc(Long reportId);
}
