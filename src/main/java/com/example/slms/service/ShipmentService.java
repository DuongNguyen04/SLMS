package com.example.slms.service;

import com.example.slms.dto.request.ShipmentUpdateRequest;
import com.example.slms.dto.response.ShipmentResponse;

public interface ShipmentService {

	ShipmentResponse getShipment(String orderId, String requesterUsername, boolean canViewAll);

	ShipmentResponse updateShipment(String orderId, ShipmentUpdateRequest request);
}
