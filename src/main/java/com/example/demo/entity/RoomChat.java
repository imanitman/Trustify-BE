package com.example.demo.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class RoomChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "roomChat", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Message> messages;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_business_id", unique = true, foreignKey = @ForeignKey(name = "fk_roomchat_user_business"))
    private UserBusiness userBusiness;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_roomchat_user"))
    @JsonIgnore
    private User user;
}
