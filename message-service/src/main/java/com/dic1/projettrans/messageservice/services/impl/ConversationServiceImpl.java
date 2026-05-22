package com.dic1.projettrans.messageservice.services.impl;

import com.dic1.projettrans.messageservice.dto.ConversationDTO;
import com.dic1.projettrans.messageservice.dto.MessageDTO;
import com.dic1.projettrans.messageservice.dto.StartConversationDTO;
import com.dic1.projettrans.messageservice.entities.Conversation;
import com.dic1.projettrans.messageservice.entities.Message;
import com.dic1.projettrans.messageservice.repositories.ConversationRepository;
import com.dic1.projettrans.messageservice.repositories.MessageRepository;
import com.dic1.projettrans.messageservice.services.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public ConversationDTO startConversation(StartConversationDTO dto, String buyerEmail) {
        // Idempotent: reuse existing conversation for same product+buyer+seller
        Conversation conversation = conversationRepository
                .findByProductIdAndBuyerEmailAndSellerEmail(dto.getProductId(), buyerEmail, dto.getSellerEmail())
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .productId(dto.getProductId())
                                .productName(dto.getProductName())
                                .productImageUrl(dto.getProductImageUrl())
                                .buyerEmail(buyerEmail)
                                .sellerEmail(dto.getSellerEmail())
                                .createdAt(Instant.now())
                                .build()
                ));

        // Send first message if provided
        if (dto.getFirstMessage() != null && !dto.getFirstMessage().isBlank()) {
            persistAndBroadcast(conversation, dto.getFirstMessage(), buyerEmail);
        }

        return toDTO(conversation, buyerEmail);
    }

    @Override
    public List<ConversationDTO> getMyConversations(String email) {
        return conversationRepository.findByParticipant(email)
                .stream()
                .map(c -> toDTO(c, email))
                .sorted((a, b) -> {
                    if (a.getLastMessageAt() == null) return 1;
                    if (b.getLastMessageAt() == null) return -1;
                    return b.getLastMessageAt().compareTo(a.getLastMessageAt());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getMessages(String conversationId, String callerEmail) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
        if (!callerEmail.equals(conversation.getBuyerEmail()) && !callerEmail.equals(conversation.getSellerEmail())) {
            throw new AccessDeniedException("Access denied to conversation " + conversationId);
        }
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId)
                .stream()
                .map(m -> toMessageDTO(m, callerEmail))
                .collect(Collectors.toList());
    }

    @Override
    public MessageDTO sendMessage(String conversationId, String content, String senderEmail) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
        if (!senderEmail.equals(conversation.getBuyerEmail()) && !senderEmail.equals(conversation.getSellerEmail())) {
            throw new AccessDeniedException("Not a participant of conversation " + conversationId);
        }
        return persistAndBroadcast(conversation, content, senderEmail);
    }

    @Override
    public void markAsRead(String conversationId, String readerEmail) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
        if (!readerEmail.equals(conv.getBuyerEmail()) && !readerEmail.equals(conv.getSellerEmail())) {
            throw new AccessDeniedException("Access denied to conversation " + conversationId);
        }

        List<Message> unread = messageRepository
                .findByConversationIdAndReadFalseAndSenderEmailNot(conversationId, readerEmail);

        unread.forEach(m -> m.setRead(true));
        messageRepository.saveAll(unread);

        // Reset unread counter on conversation
        conversationRepository.findById(conversationId).ifPresent(c -> {
            if (readerEmail.equals(c.getBuyerEmail())) {
                c.setUnreadBuyer(0);
            } else if (readerEmail.equals(c.getSellerEmail())) {
                c.setUnreadSeller(0);
            }
            conversationRepository.save(c);
        });
    }

    @Override
    public int getTotalUnreadCount(String email) {
        return conversationRepository.findByParticipant(email)
                .stream()
                .mapToInt(c -> email.equals(c.getBuyerEmail()) ? c.getUnreadBuyer() : c.getUnreadSeller())
                .sum();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private MessageDTO persistAndBroadcast(Conversation conversation, String content, String senderEmail) {
        Message message = messageRepository.save(
                Message.builder()
                        .conversationId(conversation.getId())
                        .senderEmail(senderEmail)
                        .content(content)
                        .timestamp(Instant.now())
                        .read(false)
                        .build()
        );

        // Update conversation metadata
        conversation.setLastMessagePreview(content.length() > 80 ? content.substring(0, 80) + "…" : content);
        conversation.setLastMessageAt(message.getTimestamp());

        boolean senderIsBuyer = senderEmail.equals(conversation.getBuyerEmail());
        if (senderIsBuyer) {
            conversation.setUnreadSeller(conversation.getUnreadSeller() + 1);
        } else {
            conversation.setUnreadBuyer(conversation.getUnreadBuyer() + 1);
        }
        conversationRepository.save(conversation);

        // Broadcast to conversation topic (both participants receive it)
        MessageDTO dto = toMessageDTO(message, senderEmail);
        messagingTemplate.convertAndSend("/topic/conversation/" + conversation.getId(), dto);

        // Also notify the recipient in their personal queue (for unread badge updates)
        String recipientEmail = senderIsBuyer ? conversation.getSellerEmail() : conversation.getBuyerEmail();
        messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/messages", dto);

        return dto;
    }

    private ConversationDTO toDTO(Conversation c, String callerEmail) {
        boolean isBuyer = callerEmail.equals(c.getBuyerEmail());
        int unread = isBuyer ? c.getUnreadBuyer() : c.getUnreadSeller();
        String other = isBuyer ? c.getSellerEmail() : c.getBuyerEmail();

        return ConversationDTO.builder()
                .id(c.getId())
                .productId(c.getProductId())
                .productName(c.getProductName())
                .productImageUrl(c.getProductImageUrl())
                .buyerEmail(c.getBuyerEmail())
                .sellerEmail(c.getSellerEmail())
                .otherParticipantEmail(other)
                .lastMessagePreview(c.getLastMessagePreview())
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount(unread)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private MessageDTO toMessageDTO(Message m, String callerEmail) {
        return MessageDTO.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderEmail(m.getSenderEmail())
                .content(m.getContent())
                .timestamp(m.getTimestamp())
                .read(m.isRead())
                .mine(callerEmail.equals(m.getSenderEmail()))
                .build();
    }
}
