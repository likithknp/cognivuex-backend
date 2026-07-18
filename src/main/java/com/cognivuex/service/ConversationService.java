package com.cognivuex.service;

import com.cognivuex.dto.ConversationMessage;
import com.cognivuex.entity.Conversation;
import com.cognivuex.entity.HealthReport;
import com.cognivuex.entity.User;
import com.cognivuex.repository.ConversationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);
    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    /**
     * Get or create a conversation for a user and report
     */
    public Conversation getOrCreateConversation(User user, HealthReport report) {
        Optional<Conversation> existing = conversationRepository
            .findFirstByUserAndReportOrderByUpdatedAtDesc(user, report.getId());

        if (existing.isPresent()) {
            return existing.get();
        }

        Conversation newConversation = Conversation.builder()
            .user(user)
            .report(report)
            .conversationHistory("[]") // Start with empty array
            .build();

        return conversationRepository.save(newConversation);
    }

    /**
     * Load conversation history for a conversation
     */
    public List<ConversationMessage> getConversationHistory(Conversation conversation) {
        try {
            if (conversation.getConversationHistory() == null || 
                conversation.getConversationHistory().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(
                conversation.getConversationHistory(),
                new TypeReference<List<ConversationMessage>>() {}
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize conversation history", e);
            return new ArrayList<>();
        }
    }

    /**
     * Add a message to conversation history and save
     */
    public void addMessageToConversation(Conversation conversation, String role, String content) {
        try {
            List<ConversationMessage> history = getConversationHistory(conversation);
            history.add(new ConversationMessage(role, content));
            String serialized = objectMapper.writeValueAsString(history);
            conversation.setConversationHistory(serialized);
            conversationRepository.save(conversation);
        } catch (JsonProcessingException e) {
            log.error("Failed to save conversation message", e);
        }
    }

    /**
     * Add both user and assistant messages to conversation
     */
    public void addExchangeToConversation(Conversation conversation, String userMessage, String assistantMessage) {
        addMessageToConversation(conversation, "user", userMessage);
        addMessageToConversation(conversation, "assistant", assistantMessage);
    }

    /**
     * Get the most recent conversation for a user
     */
    public Optional<Conversation> getLatestConversation(User user) {
        return conversationRepository.findFirstByUserOrderByUpdatedAtDesc(user);
    }

    /**
     * Get all conversations for a user
     */
    public List<Conversation> getUserConversations(User user) {
        return conversationRepository.findAllByUserOrderByUpdatedAtDesc(user);
    }
}
