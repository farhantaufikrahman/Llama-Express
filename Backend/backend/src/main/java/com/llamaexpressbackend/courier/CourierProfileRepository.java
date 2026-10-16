package com.llamaexpressbackend.courier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourierProfileRepository extends JpaRepository<CourierProfile, Long> {

    boolean existsByPlateNumber(String plateNumber);

    /** EntityGraph: ambil profil + user dalam 1 query JOIN, bukan 1 + N query */
    @EntityGraph(attributePaths = "user")
    Optional<CourierProfile> findWithUserByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    Page<CourierProfile> findByVerificationStatus(VerificationStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CourierProfile p where p.userId = :userId")
    Optional<CourierProfile> findForUpdate(@Param("userId") Long userId);
}