package com.example.slms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.slms.dto.request.ShipmentUpdateRequest;
import com.example.slms.dto.response.ShipmentResponse;
import com.example.slms.entity.CustomerOrder;
import com.example.slms.entity.Shipment;
import com.example.slms.entity.enums.ShipmentStatus;
import com.example.slms.exception.ValidationException;
import com.example.slms.mapper.ShipmentMapper;
import com.example.slms.repository.ShipmentRepository;
import com.example.slms.service.impl.ShipmentServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentMapper shipmentMapper;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    @Test
    void updateShipmentShouldRejectInvalidTransition() {
        Shipment shipment = Shipment.builder()
                .customerOrder(CustomerOrder.builder().orderId("ORD-001").customerUsername("alice").build())
                .status(ShipmentStatus.CREATED)
                .currentLocation("Warehouse")
                .build();

        when(shipmentRepository.findByCustomerOrderOrderId("ORD-001")).thenReturn(Optional.of(shipment));

        ShipmentUpdateRequest request = new ShipmentUpdateRequest();
        request.setStatus(ShipmentStatus.DELIVERED);

        assertThrows(ValidationException.class, () -> shipmentService.updateShipment("ORD-001", request));
    }

    @Test
    void updateShipmentShouldAllowCreatedToInTransit() {
        Shipment shipment = Shipment.builder()
                .customerOrder(CustomerOrder.builder().orderId("ORD-002").customerUsername("alice").build())
                .status(ShipmentStatus.CREATED)
                .currentLocation("Warehouse")
                .build();

        when(shipmentRepository.findByCustomerOrderOrderId("ORD-002")).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentMapper.toResponse(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment value = invocation.getArgument(0);
            return ShipmentResponse.builder()
                    .orderId(value.getCustomerOrder().getOrderId())
                    .status(value.getStatus())
                    .currentLocation(value.getCurrentLocation())
                    .build();
        });

        ShipmentUpdateRequest request = new ShipmentUpdateRequest();
        request.setStatus(ShipmentStatus.IN_TRANSIT);
        request.setCurrentLocation("Distribution Center");

        ShipmentResponse response = shipmentService.updateShipment("ORD-002", request);

        assertEquals(ShipmentStatus.IN_TRANSIT, response.getStatus());
        assertEquals("Distribution Center", response.getCurrentLocation());
    }
}
