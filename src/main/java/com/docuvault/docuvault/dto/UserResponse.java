package com.docuvault.docuvault.dto;

import com.docuvault.docuvault.entity.User;
import lombok.Getter;

@Getter
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private User.Role role;

    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }
}