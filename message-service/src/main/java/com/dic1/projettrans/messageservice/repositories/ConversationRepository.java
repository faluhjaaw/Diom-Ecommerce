package com.dic1.projettrans.messageservice.repositories;

import com.dic1.projettrans.messageservice.entities.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    /** Find existing conversation between buyer and seller for a specific product */
    Optional<Conversation> findByProductIdAndBuyerEmailAndSellerEmail(
            String productId, String buyerEmail, String sellerEmail);

    /** All conversations where user is buyer OR seller, sorted by last message */
    @Query("{ '$or': [ { 'buyerEmail': ?0 }, { 'sellerEmail': ?0 } ] }")
    List<Conversation> findByParticipant(String email);
}
