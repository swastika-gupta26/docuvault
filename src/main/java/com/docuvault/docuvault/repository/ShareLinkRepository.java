package com.docuvault.docuvault.repository;

import com.docuvault.docuvault.entity.Document;
import com.docuvault.docuvault.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByToken(String token);
    void deleteByDocument(Document document);
}