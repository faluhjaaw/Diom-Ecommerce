package com.dic1.projettrans.messageservice.repositories;

import com.dic1.projettrans.messageservice.entities.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByConversationIdOrderByTimestampAsc(String conversationId);

    int countByConversationIdAndReadFalseAndSenderEmailNot(String conversationId, String readerEmail);

    List<Message> findByConversationIdAndReadFalseAndSenderEmailNot(String conversationId, String readerEmail);
}
