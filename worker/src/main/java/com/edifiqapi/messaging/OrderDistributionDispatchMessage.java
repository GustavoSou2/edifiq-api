package com.edifiqapi.messaging;

public record OrderDistributionDispatchMessage(
        Long distributionId,
        Long orderId,
        Long supplierId,
        String supplierName,
        String supplierEmail,
        String supplierPhone
) {}
