package com.docuvault.docuvault.service;

import com.docuvault.docuvault.dto.ShareLinkRequest;
import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.ShareLink;
import com.docuvault.docuvault.entity.User;
import com.docuvault.docuvault.repository.DocumentRepository;
import com.docuvault.docuvault.repository.ShareLinkRepository;
import com.docuvault.docuvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareLinkServiceTest {

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ShareLinkService shareLinkService;

    private User owner;
    private User otherUser;
    private Document document;

    @BeforeEach
    void setUp() {

        owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        document = new Document();
        document.setId(10L);
        document.setTitle("Test Document");
        document.setOwner(owner);
    }

    // ---------------------------------------------------------
    // CREATE SHARE LINK
    // ---------------------------------------------------------

    @Test
    void createShareLink_shouldCreateLink() {

        ShareLinkRequest request = new ShareLinkRequest();
        request.setExpiresAt(
                LocalDateTime.now().plusDays(1)
        );
        request.setOneTimeUse(false);
        request.setPassword("secret123");

        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(owner));

        when(documentRepository.findById(10L))
                .thenReturn(Optional.of(document));

        when(passwordEncoder.encode("secret123"))
                .thenReturn("hashedPassword");

        ShareLink savedLink = new ShareLink();
        savedLink.setDocument(document);
        savedLink.setPasswordHash("hashedPassword");

        when(shareLinkRepository.save(any(ShareLink.class)))
                .thenReturn(savedLink);

        ShareLink result =
                shareLinkService.createShareLink(
                        10L,
                        request,
                        "owner@example.com"
                );

        assertNotNull(result);
        assertEquals(document, result.getDocument());
        assertEquals("hashedPassword", result.getPasswordHash());

        verify(passwordEncoder)
                .encode("secret123");

        verify(shareLinkRepository)
                .save(any(ShareLink.class));
    }

    @Test
    void createShareLink_shouldCreateLinkWithoutPassword() {

        ShareLinkRequest request = new ShareLinkRequest();
        request.setExpiresAt(
                LocalDateTime.now().plusDays(1)
        );
        request.setOneTimeUse(false);
        request.setPassword(null);

        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(owner));

        when(documentRepository.findById(10L))
                .thenReturn(Optional.of(document));

        when(shareLinkRepository.save(any(ShareLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShareLink result =
                shareLinkService.createShareLink(
                        10L,
                        request,
                        "owner@example.com"
                );

        assertNotNull(result);
        assertEquals(document, result.getDocument());
        assertNull(result.getPasswordHash());

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(shareLinkRepository)
                .save(any(ShareLink.class));
    }

    @Test
    void createShareLink_shouldThrowExceptionWhenUserNotFound() {

        ShareLinkRequest request = new ShareLinkRequest();

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> shareLinkService.createShareLink(
                        10L,
                        request,
                        "unknown@example.com"
                )
        );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );

        verify(documentRepository, never())
                .findById(anyLong());
    }

    @Test
    void createShareLink_shouldThrowExceptionWhenDocumentNotFound() {

        ShareLinkRequest request = new ShareLinkRequest();

        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(owner));

        when(documentRepository.findById(10L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> shareLinkService.createShareLink(
                        10L,
                        request,
                        "owner@example.com"
                )
        );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    @Test
    void createShareLink_shouldThrowExceptionForNonOwner() {

        ShareLinkRequest request = new ShareLinkRequest();

        when(userRepository.findByEmail("other@example.com"))
                .thenReturn(Optional.of(otherUser));

        when(documentRepository.findById(10L))
                .thenReturn(Optional.of(document));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> shareLinkService.createShareLink(
                        10L,
                        request,
                        "other@example.com"
                )
        );

        assertEquals(
                403,
                exception.getStatusCode().value()
        );

        verify(shareLinkRepository, never())
                .save(any(ShareLink.class));
    }

    // ---------------------------------------------------------
    // GET SHARE LINK
    // ---------------------------------------------------------

    @Test
    void getShareLink_shouldReturnValidLink() {

        ShareLink shareLink = new ShareLink();
        shareLink.setDocument(document);
        shareLink.setToken("abc123");
        shareLink.setExpiresAt(
                LocalDateTime.now().plusDays(1)
        );
        shareLink.setOneTimeUse(false);
        shareLink.setUsed(false);

        when(shareLinkRepository.findByToken("abc123"))
                .thenReturn(Optional.of(shareLink));

        ShareLink result =
                shareLinkService.getShareLink("abc123");

        assertEquals(shareLink, result);
    }

    @Test
    void getShareLink_shouldThrowExceptionWhenNotFound() {

        when(shareLinkRepository.findByToken("invalid"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> shareLinkService.getShareLink("invalid")
        );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    @Test
    void getShareLink_shouldThrowExceptionWhenExpired() {

        ShareLink shareLink = new ShareLink();
        shareLink.setExpiresAt(
                LocalDateTime.now().minusMinutes(1)
        );
        shareLink.setOneTimeUse(false);

        when(shareLinkRepository.findByToken("expired"))
                .thenReturn(Optional.of(shareLink));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> shareLinkService.getShareLink("expired")
        );

        assertEquals(
                410,
                exception.getStatusCode().value()
        );
    }

    @Test
    void getShareLink_shouldThrowExceptionWhenAlreadyUsed() {

        ShareLink shareLink = new ShareLink();
        shareLink.setExpiresAt(null);
        shareLink.setOneTimeUse(true);
        shareLink.setUsed(true);

        when(shareLinkRepository.findByToken("used"))
                .thenReturn(Optional.of(shareLink));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> shareLinkService.getShareLink("used")
        );

        assertEquals(
                410,
                exception.getStatusCode().value()
        );
    }

    // ---------------------------------------------------------
    // ACCESS SHARE LINK
    // ---------------------------------------------------------

    @Test
    void accessShareLink_shouldReturnLinkWithCorrectPassword() {

        ShareLink shareLink = new ShareLink();
        shareLink.setToken("abc123");
        shareLink.setPasswordHash("hashedPassword");
        shareLink.setOneTimeUse(false);
        shareLink.setUsed(false);

        when(shareLinkRepository.findByToken("abc123"))
                .thenReturn(Optional.of(shareLink));

        when(passwordEncoder.matches(
                "secret123",
                "hashedPassword"
        )).thenReturn(true);

        ShareLink result =
                shareLinkService.accessShareLink(
                        "abc123",
                        "secret123"
                );

        assertEquals(shareLink, result);

        verify(passwordEncoder)
                .matches("secret123", "hashedPassword");
    }

    @Test
    void accessShareLink_shouldThrowExceptionForWrongPassword() {

        ShareLink shareLink = new ShareLink();
        shareLink.setPasswordHash("hashedPassword");
        shareLink.setOneTimeUse(false);
        shareLink.setUsed(false);

        when(shareLinkRepository.findByToken("abc123"))
                .thenReturn(Optional.of(shareLink));

        when(passwordEncoder.matches(
                "wrongPassword",
                "hashedPassword"
        )).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> shareLinkService.accessShareLink(
                        "abc123",
                        "wrongPassword"
                )
        );

        assertEquals(
                401,
                exception.getStatusCode().value()
        );
    }

    @Test
    void accessShareLink_shouldAllowLinkWithoutPassword() {

        ShareLink shareLink = new ShareLink();
        shareLink.setPasswordHash(null);
        shareLink.setOneTimeUse(false);
        shareLink.setUsed(false);

        when(shareLinkRepository.findByToken("abc123"))
                .thenReturn(Optional.of(shareLink));

        ShareLink result =
                shareLinkService.accessShareLink(
                        "abc123",
                        null
                );

        assertEquals(shareLink, result);

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());
    }

    @Test
    void accessShareLink_shouldMarkOneTimeLinkAsUsed() {

        ShareLink shareLink = new ShareLink();
        shareLink.setPasswordHash(null);
        shareLink.setOneTimeUse(true);
        shareLink.setUsed(false);

        when(shareLinkRepository.findByToken("abc123"))
                .thenReturn(Optional.of(shareLink));

        when(shareLinkRepository.save(shareLink))
                .thenReturn(shareLink);

        ShareLink result =
                shareLinkService.accessShareLink(
                        "abc123",
                        null
                );

        assertEquals(shareLink, result);
        assertTrue(shareLink.isUsed());

        verify(shareLinkRepository)
                .save(shareLink);
    }
}