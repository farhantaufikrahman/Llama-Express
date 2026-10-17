package com.llamaexpressbackend.address;

import com.llamaexpressbackend.common.persistance.BaseEntity;
import com.llamaexpressbackend.order.GeoPoint;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String label;

    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "address_line", nullable = false, length = 255))
    @AttributeOverride(name = "lat", column = @Column(name = "lat", nullable = false, precision = 9, scale = 6))
    @AttributeOverride(name = "lng", column = @Column(name = "lng", nullable = false, precision = 9, scale = 6))
    private GeoPoint location;

    @Column(length = 255)
    private String notes;

    public static Address create(Long userId, String label, GeoPoint location, String notes) {
        Address a = new Address();
        a.userId = userId;
        a.update(label, location, notes);
        return a;
    }

    public void update(String label, GeoPoint location, String notes) {
        this.label = label.trim();
        this.location = location;
        this.notes = (notes == null || notes.isBlank()) ? null : notes.trim();
    }
}