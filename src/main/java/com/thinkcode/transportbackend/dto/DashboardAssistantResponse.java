package com.thinkcode.transportbackend.dto;

import java.util.List;

public record DashboardAssistantResponse(
    Integer assignedTasks,
    Integer completedToday,
    Integer pendingApprovals,
    List<String> upcomingSchedules,
    Long unreadMessages
) {}
