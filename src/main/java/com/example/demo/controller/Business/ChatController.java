package com.example.demo.controller.Business;

import com.trustify.trustify.dto.Req.ChatMessage;
import com.trustify.trustify.dto.Res.OutgoingMessage;
import com.trustify.trustify.dto.Res.RoomChatDTO;
import com.trustify.trustify.entity.Message;
import com.trustify.trustify.entity.RoomChat;
import com.trustify.trustify.entity.UserBusiness;
import com.trustify.trustify.enums.NotificationType;
import com.trustify.trustify.repository.Business.MessageRepository;
import com.trustify.trustify.repository.Business.RoomChatRepository;
import com.trustify.trustify.service.Business.UserBusinessService;
import com.trustify.trustify.service.NotificationService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@AllArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final RoomChatRepository roomChatRepository;
    private final UserBusinessService userBusinessService;
    private final NotificationService notificationService;

    /**
     * STOMP endpoint: send to /app/business/{roomId}
     * - payload: ChatMessage (roomId optional if path variable provided)
     * - sender: Principal.getName() when available
     * - creates RoomChat if missing
     * - persists Message and broadcasts to /topic/rooms/{roomId}
     */
    @MessageMapping("/business/{roomId}")
    public void sendToRoom(@DestinationVariable Long roomId,
                           @Payload ChatMessage payload,
                           Principal principal) {
        if (payload == null || payload.getMessage() == null || payload.getMessage().trim().isEmpty()) {
            return;
        }

        Long effectiveRoomId = roomId != null ? roomId : payload.getRoomId();

        RoomChat room = (effectiveRoomId != null)
                ? roomChatRepository.findById(effectiveRoomId)
                .orElseGet(() -> {
                    UserBusiness userBusiness = userBusinessService.findByEmail(principal.getName());
                    RoomChat rc = new RoomChat();
                    rc.setName(principal.getName());
                    rc.setUserBusiness(userBusiness);
                    userBusiness.setRoomChat(rc);
                    return roomChatRepository.save(rc);
                })
                : createNewRoom(principal);

        Message m = new Message();
        m.setMessage(payload.getMessage());
        m.setSender(principal != null ? principal.getName() : "anonymous");
        m.setRoomChat(room);
        m.setRead(false);
        m.setAdmin(payload.isAdmin());
        m.setDeleted(false);

        Message saved = messageRepository.save(m);
        OutgoingMessage out = new OutgoingMessage();
        out.setId(saved.getId());
        out.setRoomId(room.getId());
        out.setSender(saved.getSender());
        out.setMessage(saved.getMessage());
        out.setAdmin(saved.isAdmin());
        out.setTimestamp(Instant.now());

        // ✅ BROADCAST TRƯỚC - quan trọng nhất!
        System.out.println("📢 Broadcasting to /topic/rooms/" + room.getId() + ": " + out.getMessage());
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), out);

        // ✅ Notification SAU và wrap trong try-catch
        try {
            if (out.isAdmin()) {
                // Admin gửi → thông báo Business user của room này
                UserBusiness businessUser = room.getUserBusiness();
                if (businessUser != null) {
                    notificationService.sendToUserBusiness(businessUser, "Bạn có tin nhắn mới từ Admin", NotificationType.MESSAGE);
                }
            } else {
                // Business gửi → thông báo Admin
                notificationService.sendToAdmin("Tin nhắn hỗ trợ mới", NotificationType.MESSAGE);
            }
        } catch (Exception e) {
            System.err.println("❌ Notification error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private RoomChat createNewRoom(Principal principal) {
        UserBusiness userBusiness = userBusinessService.findByEmail(principal.getName());
        RoomChat rc = new RoomChat();
        rc.setName(principal.getName());
        rc.setUserBusiness(userBusiness);
        // Thiết lập quan hệ hai chiều
        userBusiness.setRoomChat(rc);
        return roomChatRepository.save(rc);
    }


    /**
     * REST: load history for a room
     */
    @GetMapping("/rooms/{roomId}/messages")
    public List<OutgoingMessage> getRoomMessages(@PathVariable Long roomId) {
        List<Message> msgs = messageRepository.findAllByRoomChatIdOrderByDateAsc(roomId);
        return msgs.stream()
                .map(e -> {
                    OutgoingMessage o = new OutgoingMessage();
                    o.setId(e.getId());
                    o.setRoomId(roomId);
                    o.setSender(e.getSender());
                    o.setMessage(e.getMessage());
                    o.setAdmin(e.isAdmin()); // assumes entity getter is isAdmin()
                    o.setTimestamp(e.getDate() != null
                            ? e.getDate().atZone(java.time.ZoneId.systemDefault()).toInstant()
                            : Instant.now());
                    return o;
                })
                .collect(Collectors.toList());
    }

    /**
     * REST: get all rooms (Admin only)
     */
    @GetMapping("/rooms")
    public List<RoomChatDTO> getAllRooms() {
        List<RoomChat> rooms = roomChatRepository.findAll();
        return rooms.stream()
                .map(room -> {
                    RoomChatDTO dto = new RoomChatDTO();
                    dto.setId(room.getId());
                    dto.setName(room.getName());
                    dto.setUserBusinessId(room.getUserBusiness() != null
                            ? room.getUserBusiness().getId()
                            : null);
                    dto.setUserBusinessName(room.getUserBusiness() != null
                            ? room.getUserBusiness().getName()
                            : null);
                    dto.setMessageCount(room.getMessages() != null
                            ? room.getMessages().size()
                            : 0);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * REST: get current user's room
     */
    @GetMapping("/my-room")
    public RoomChatDTO getMyRoom(Principal principal) {
        if (principal == null) {
            return null;
        }
        UserBusiness userBusiness = userBusinessService.findByEmail(principal.getName());
        if (userBusiness == null || userBusiness.getRoomChat() == null) {
            return null;
        }
        RoomChat room = userBusiness.getRoomChat();
        RoomChatDTO dto = new RoomChatDTO();
        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setUserBusinessId(userBusiness.getId());
        dto.setUserBusinessName(userBusiness.getName());
        dto.setMessageCount(room.getMessages() != null
                ? room.getMessages().size()
                : 0);
        return dto;
    }
}
