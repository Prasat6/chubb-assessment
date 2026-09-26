package com.chubb.claims.web;

import com.chubb.claims.domain.User;
import com.chubb.claims.dto.Dtos.NotificationDto;
import com.chubb.claims.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationDto> mine(@CurrentUser User user) {
        return notificationService.myNotifications(user);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@CurrentUser User user) {
        return Map.of("count", notificationService.unreadCount(user));
    }

    @PostMapping("/{id}/read")
    public void markRead(@CurrentUser User user, @PathVariable Long id) {
        notificationService.markRead(user, id);
    }

    @PostMapping("/read-all")
    public void markAllRead(@CurrentUser User user) {
        notificationService.markAllRead(user);
    }
}
