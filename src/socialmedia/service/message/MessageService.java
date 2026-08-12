package socialmedia.service.message;

import socialmedia.model.Message;
import socialmedia.model.MessageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageService {
    private static volatile MessageService instance;

    // userId -> every message where this user is sender or receiver
    private final Map<Integer, List<Message>> messagesByUserId = new HashMap<>();
    // canonical "smaller_larger" userId pair -> messages exchanged between those two users
    private final Map<String, List<Message>> conversationMap = new HashMap<>();

    private MessageService() {
    }

    public static MessageService getMessageService(){
        if (instance == null){
            synchronized (MessageService.class){
                if (instance == null){
                    instance = new MessageService();
                }
            }
        }
        return instance;
    }

    public synchronized Message sendMessage(MessageRequest messageRequest){
        if (messageRequest.getSenderId() == messageRequest.getReceiverId()){
            throw new IllegalArgumentException("Cannot send a message to yourself");
        }
        Message message = new Message(messageRequest.getSenderId(), messageRequest.getReceiverId(), messageRequest.getContent());
        messagesByUserId.computeIfAbsent(message.getSenderId(), id -> new ArrayList<>()).add(message);
        messagesByUserId.computeIfAbsent(message.getReceiverId(), id -> new ArrayList<>()).add(message);
        conversationMap.computeIfAbsent(conversationKey(message.getSenderId(), message.getReceiverId()), key -> new ArrayList<>())
                .add(message);
        return message;
    }

    public synchronized List<Message> getConversation(int senderId, int receiverId){
        List<Message> conversation = conversationMap.getOrDefault(conversationKey(senderId, receiverId), Collections.emptyList());
        return new ArrayList<>(conversation);
    }

    public synchronized List<Message> getAllConversations(int userId){
        List<Message> messages = new ArrayList<>(messagesByUserId.getOrDefault(userId, Collections.emptyList()));
        messages.sort(Comparator.comparing(Message::getSentAt));
        return messages;
    }

    private String conversationKey(int userA, int userB){
        return userA < userB ? userA + "_" + userB : userB + "_" + userA;
    }
}
