package com.llamaexpressbackend.admin;

import com.llamaexpressbackend.admin.dto.AdminOrderFilter;
import com.llamaexpressbackend.order.DeliveryOrder;
import com.llamaexpressbackend.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AdminOrderSpecifications {

    private AdminOrderSpecifications() {
    }

    /**
     * Setiap filter yang diisi menambah satu syarat WHERE. Filter kosong =
     * diabaikan.
     * afterId (opsional) dipakai ekspor CSV untuk keyset pagination: WHERE id >
     * afterId.
     */
    public static Specification<DeliveryOrder> matching(AdminOrderFilter f, ZoneId zone, Long afterId) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.status() != null) {
                p.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.serviceType() != null) {
                p.add(cb.equal(root.get("serviceType"), f.serviceType()));
            }
            if (f.paymentMethod() != null) {
                p.add(cb.equal(root.get("paymentMethod"), f.paymentMethod()));
            }
            if (f.from() != null) {
                p.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), f.from().atStartOfDay(zone).toInstant()));
            }
            if (f.to() != null) {
                p.add(cb.lessThan(root.<Instant>get("createdAt"), f.to().plusDays(1).atStartOfDay(zone).toInstant()));
            }
            if (f.q() != null && !f.q().isBlank()) {
                // Pencocokan TEPAT (bukan LIKE '%...%') → bisa memakai index unique
                String q = f.q().trim();
                Join<DeliveryOrder, User> customer = root.join("customer");
                p.add(cb.or(
                        cb.equal(root.get("orderCode"), q.toUpperCase(Locale.ROOT)),
                        cb.equal(customer.get("email"), q.toLowerCase(Locale.ROOT)),
                        cb.equal(customer.get("phone"), q)));
            }
            if (afterId != null) {
                p.add(cb.greaterThan(root.<Long>get("id"), afterId));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}