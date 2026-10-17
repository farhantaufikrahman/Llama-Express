package com.llamaexpressbackend.tracking;

import com.llamaexpressbackend.order.DeliveryOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TrackingAccessService {

    private static final Pattern ORDER_TOPIC = Pattern.compile("^/topic/orders/([A-Z0-9-]+)/(status|location)$");

    private final DeliveryOrderRepository orderRepository;

    /**
     * Hanya customer pemilik, kurir yang ditugaskan, atau admin yang boleh
     * berlangganan
     */
    @Transactional(readOnly = true)
    public boolean canSubscribe(Authentication auth, String destination) {
        if (auth == null || destination == null) {
            return false;
        }
        Matcher m = ORDER_TOPIC.matcher(destination);
        if (!m.matches()) {
            return false; // tujuan lain tidak diizinkan
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin || orderRepository.isParticipant(m.group(1), Long.valueOf(auth.getName()));
    }
}