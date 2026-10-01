package com.docuvault.docuvault.controller;

import com.docuvault.docuvault.dto.ShareLinkRequest;
import com.docuvault.docuvault.entity.ShareLink;
import com.docuvault.docuvault.service.ShareLinkService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareLinkControllerTest {

    @Mock
    private ShareLinkService shareLinkService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private Principal principal;

    @InjectMocks
    private ShareLinkController shareLinkController;

    @Test
    void createShareLink_shouldReturnShareLinkResponse() {

        ShareLinkRequest request = new ShareLinkRequest();

        ShareLink shareLink = new ShareLink();

        when(httpRequest.getUserPrincipal())
                .thenReturn(principal);

        when(principal.getName())
                .thenReturn("owner@example.com");

        when(shareLinkService.createShareLink(
                1L,
                request,
                "owner@example.com"
        )).thenReturn(shareLink);

        ResponseEntity<?> response =
                shareLinkController.createShareLink(
                        1L,
                        request,
                        httpRequest
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        verify(shareLinkService)
                .createShareLink(
                        1L,
                        request,
                        "owner@example.com"
                );
    }

    @Test
    void accessShareLink_shouldReturnShareLinkResponse() {

        ShareLink shareLink = new ShareLink();

        when(shareLinkService.accessShareLink(
                "abc123",
                "password"
        )).thenReturn(shareLink);

        ResponseEntity<?> response =
                shareLinkController.accessShareLink(
                        "abc123",
                        "password"
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        verify(shareLinkService)
                .accessShareLink(
                        "abc123",
                        "password"
                );
    }
}