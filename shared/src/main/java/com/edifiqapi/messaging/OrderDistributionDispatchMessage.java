package com.edifiqapi.messaging;

public record OrderDistributionDispatchMessage(
        String distributionId,
        String orderId,
        String supplierId,
        String supplierName,
        String supplierEmail,
        String supplierPhone
) {}
