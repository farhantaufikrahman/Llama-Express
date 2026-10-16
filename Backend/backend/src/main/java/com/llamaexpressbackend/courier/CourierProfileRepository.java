package com.llamaexpressbackend.courier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourierProfileRepository extends JpaRepository<CourierProfile, Long> {

    boolean existsByPlateNumber(String plateNumber);

    /** EntityGraph: ambil profil + user dalam 1 query JOIN, bukan 1 + N query */
    @EntityGraph(attributePaths = "user")
    Optional<CourierProfile> findWithUserByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    Page<CourierProfile> findByVerificationStatus(VerificationStatus status, Pageable pageable);
}