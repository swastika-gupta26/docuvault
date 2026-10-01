package com.docuvault.docuvault.dto;

import com.docuvault.docuvault.entity.ShareLink;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ShareLinkResponse {

    private String token;
    private LocalDateTime expiresAt;
    private boolean oneTimeUse;

    public ShareLinkResponse(ShareLink shareLink) {
        this.token = shareLink.getToken();
        this.expiresAt = shareLink.getExpiresAt();
        this.oneTimeUse = shareLink.isOneTimeUse();
    }
}