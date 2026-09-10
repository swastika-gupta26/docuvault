package com.docuvault.docuvault.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentRequest {

    @NotBlank
    private String title;

    private String description;
}