package com.llamaexpressbackend.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    @Query("""
            select u from User u
            where u.role = :role
              and (:q is null or lower(u.fullName) like concat('%', :q, '%')
                   or u.email like concat('%', :q, '%') or u.phone like concat('%', :q, '%'))
            """)
    Page<User> search(@Param("role") Role role, @Param("q") String q, Pageable pageable);
}
