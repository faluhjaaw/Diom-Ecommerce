package com.dic1.projettrans.messageservice.services;

import com.dic1.projettrans.messageservice.dto.ConversationDTO;
import com.dic1.projettrans.messageservice.dto.MessageDTO;
import com.dic1.projettrans.messageservice.dto.StartConversationDTO;

import java.util.List;

public interface ConversationService {

    ConversationDTO startConversation(StartConversationDTO dto, String buyerEmail);

    List<ConversationDTO> getMyConversations(String email);

    List<MessageDTO> getMessages(String conversationId, String callerEmail);

    MessageDTO sendMessage(String conversationId, String content, String senderEmail);

    void markAsRead(String conversationId, String readerEmail);

    int getTotalUnreadCount(String email);
}
