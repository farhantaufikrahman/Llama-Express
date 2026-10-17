package com.llamaexpressbackend.user;

import com.llamaexpressbackend.common.persistance.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA butuh no-arg constructor; protected supaya tidak dipakai
                                                   // sembarangan

public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(nullable = false, length = 20, unique = true)
    private String phone;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    public static User create(Role role, String fullName, String email, String phone, String passwordHash) {
        User u = new User();
        u.role = role;
        u.fullName = fullName.trim();
        u.email = email.trim().toLowerCase(Locale.ROOT);
        u.phone = phone.trim();
        u.passwordHash = passwordHash;
        return u;
    }

    public void updateProfile(String fullName, String phone) {
        if (fullName != null)
            this.fullName = fullName.trim();
        if (phone != null)
            this.phone = phone.trim();
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

}
