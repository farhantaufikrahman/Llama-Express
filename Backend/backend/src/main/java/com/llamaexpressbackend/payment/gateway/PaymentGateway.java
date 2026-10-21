package com.llamaexpressbackend.payment.gateway;

public interface PaymentGateway {

    SnapResponse createTransaction(SnapRequest request);

    /** Status terkini langsung dari Midtrans (sumber kebenaran) */
    MidtransNotification getStatus(String merchantOrderId);
}