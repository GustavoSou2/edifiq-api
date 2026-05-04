package com.edifiqapi.messaging;

import java.util.List;

public record OrderDistributionsQueuedEvent(List<Long> distributionIds) {}
