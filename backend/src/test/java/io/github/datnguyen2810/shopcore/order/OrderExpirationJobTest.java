package io.github.datnguyen2810.shopcore.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderExpirationJobTest {

    @Mock private OrderRepository orders;
    @Mock private OrderService orderService;

    @Test
    void expireOverdueOrders_whenOneOrderFails_stillExpiresTheRest() {
        UUID failing = UUID.randomUUID();
        UUID next = UUID.randomUUID();
        when(orders.findOverduePendingPaymentIds(any(), eq(OrderExpirationJob.BATCH_SIZE)))
                .thenReturn(List.of(failing, next));
        when(orderService.expireOrder(eq(failing), any())).thenThrow(new IllegalStateException("Stock is inconsistent"));

        new OrderExpirationJob(orders, orderService).expireOverdueOrders();

        verify(orderService).expireOrder(eq(next), any());
    }
}
