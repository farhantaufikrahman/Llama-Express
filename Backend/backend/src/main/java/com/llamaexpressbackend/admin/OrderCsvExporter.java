package com.llamaexpressbackend.admin;

import com.llamaexpressbackend.admin.dto.AdminOrderFilter;
import com.llamaexpressbackend.courier.CourierProperties;
import com.llamaexpressbackend.order.DeliveryOrder;
import com.llamaexpressbackend.order.DeliveryOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderCsvExporter {

    private static final int BATCH_SIZE = 500;
    private static final int MAX_ROWS = 50_000;
    private static final String HEADER = "order_code,created_at,status,service_type,vehicle_type,payment_method,"
            + "payment_status,price_total,distance_m,customer_id,courier_id,pickup_address,dropoff_address,notes\n";

    private final DeliveryOrderRepository orderRepository;
    private final CourierProperties courierProperties;
    private final TransactionTemplate tx;

    /**
     * Ditulis per batch langsung ke response — memori tetap kecil berapa pun jumlah
     * order.
     * Setiap batch = transaksi read-only singkat + keyset pagination (WHERE id >
     * lastId ORDER BY id).
     */
    public void write(AdminOrderFilter filter, OutputStream out) throws IOException {
        ZoneId zone = courierProperties.businessZone();
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write('\uFEFF'); // BOM → Excel membaca UTF-8 dengan benar
        writer.write(HEADER);

        long lastId = 0;
        int written = 0;
        while (written < MAX_ROWS) {
            final long afterId = lastId;
            List<String[]> batch = tx.execute(s -> orderRepository.findBy(
                    AdminOrderSpecifications.matching(filter, zone, afterId),
                    q -> q.sortBy(Sort.by("id")).limit(BATCH_SIZE).all())
                    .stream().map(OrderCsvExporter::toLine).toList());
            if (batch == null || batch.isEmpty()) {
                break;
            }
            for (String[] line : batch) {
                writer.write(line[1]);
                written++;
            }
            writer.flush(); // kirim batch ini ke browser sekarang juga
            lastId = Long.parseLong(batch.getLast()[0]);
            if (batch.size() < BATCH_SIZE) {
                break;
            }
        }
        writer.flush();
    }

    /**
     * [0] = id (untuk keyset), [1] = baris CSV. customer_id/courier_id diambil dari
     * FK → tanpa query tambahan
     */
    private static String[] toLine(DeliveryOrder o) {
        String line = String.join(",",
                Csv.raw(o.getOrderCode()),
                Csv.raw(o.getCreatedAt()),
                Csv.raw(o.getStatus()),
                Csv.raw(o.getServiceType()),
                Csv.raw(o.getVehicleType()),
                Csv.raw(o.getPaymentMethod()),
                Csv.raw(o.getPaymentStatus()),
                Csv.raw(o.getPriceTotal()),
                Csv.raw(o.getDistanceMeters()),
                Csv.raw(o.getCustomer().getId()),
                Csv.raw(o.getCourier() == null ? null : o.getCourier().getId()),
                Csv.text(o.getPickup().getAddress()),
                Csv.text(o.getDropoff().getAddress()),
                Csv.text(o.getNotes())) + "\n";
        return new String[] { String.valueOf(o.getId()), line };
    }
}