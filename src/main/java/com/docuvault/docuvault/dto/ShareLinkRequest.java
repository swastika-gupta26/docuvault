package com.docuvault.docuvault.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ShareLinkRequest {

    private LocalDateTime expiresAt;

    private String password;

    private boolean oneTimeUse;
}