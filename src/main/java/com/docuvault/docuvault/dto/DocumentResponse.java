package com.docuvault.docuvault.dto;

import com.docuvault.docuvault.entity.Document;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DocumentResponse {

    private Long id;
    private String title;
    private String description;
    private UserResponse owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentResponse(Document document) {
        this.id = document.getId();
        this.title = document.getTitle();
        this.description = document.getDescription();
        this.owner = new UserResponse(document.getOwner());
        this.createdAt = document.getCreatedAt();
        this.updatedAt = document.getUpdatedAt();
    }
}