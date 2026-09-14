package com.nextset.repository;

import com.nextset.entity.BodyPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BodyPartRepository extends JpaRepository<BodyPart, UUID> {
    Optional<BodyPart> findByName(String name);
}