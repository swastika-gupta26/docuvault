package com.docuvault.docuvault.repository;

import com.docuvault.docuvault.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionRepository extends JpaRepository<Version, Long> {
}
