package com.example.slms.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.slms.dto.request.ShipmentUpdateRequest;
import com.example.slms.dto.response.ShipmentResponse;
import com.example.slms.entity.Shipment;
import com.example.slms.entity.enums.ShipmentStatus;
import com.example.slms.exception.BusinessException;
import com.example.slms.exception.ValidationException;
import com.example.slms.mapper.ShipmentMapper;
import com.example.slms.repository.ShipmentRepository;
import com.example.slms.service.ShipmentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

	private final ShipmentRepository shipmentRepository;
	private final ShipmentMapper shipmentMapper;

	@Override
	@Transactional(readOnly = true)
	public ShipmentResponse getShipment(String orderId, String requesterUsername, boolean canViewAll) {
		Shipment shipment = findShipmentByOrderIdOrThrow(orderId);
		if (!canViewAll && !shipment.getCustomerOrder().getCustomerUsername().equals(requesterUsername)) {
			throw new BusinessException("You are not allowed to access this shipment", HttpStatus.FORBIDDEN);
		}

		return shipmentMapper.toResponse(shipment);
	}

	@Override
	@Transactional
	public ShipmentResponse updateShipment(String orderId, ShipmentUpdateRequest request) {
		Shipment shipment = findShipmentByOrderIdOrThrow(orderId);

		ShipmentStatus targetStatus = request.getStatus();
		if (targetStatus == null) {
			throw new ValidationException("status is required");
		}

		ShipmentStatus currentStatus = shipment.getStatus();
		if (!isValidTransition(currentStatus, targetStatus)) {
			throw new ValidationException(
					"Invalid shipment status transition: " + currentStatus + " -> " + targetStatus);
		}

		shipment.setStatus(targetStatus);

		Shipment updatedShipment = shipmentRepository.save(shipment);
		return shipmentMapper.toResponse(updatedShipment);
	}

	private Shipment findShipmentByOrderIdOrThrow(String orderId) {
		if (orderId == null || orderId.trim().isEmpty()) {
			throw new ValidationException("orderId is required");
		}

		return shipmentRepository.findByCustomerOrderOrderId(orderId.trim())
				.orElseThrow(() -> new BusinessException("Shipment not found", HttpStatus.NOT_FOUND));
	}

	private boolean isValidTransition(ShipmentStatus currentStatus, ShipmentStatus targetStatus) {
		if (currentStatus == null || targetStatus == null || currentStatus == targetStatus) {
			return false;
		}

		return switch (currentStatus) {
			case CREATED -> targetStatus == ShipmentStatus.IN_TRANSIT;
			case IN_TRANSIT -> targetStatus == ShipmentStatus.DELIVERED;
			case DELIVERED -> false;
		};
	}
}
