package com.example.demo.repository.Business;

import com.trustify.trustify.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findAllByRoomChatIdOrderByDateAsc(Long roomId);
    List<Message> findAllByOrderByDateAsc();
}
