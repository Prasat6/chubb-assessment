package com.chubb.claims.service;

import com.chubb.claims.domain.Claim;
import com.chubb.claims.domain.Notification;
import com.chubb.claims.domain.User;
import com.chubb.claims.domain.UserRole;
import com.chubb.claims.dto.Dtos.NotificationDto;
import com.chubb.claims.repository.NotificationRepository;
import com.chubb.claims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationDispatchService dispatchService;

    public void notify(User recipient, Claim claim, String message) {
        notificationRepository.save(new Notification(recipient, claim, message));
        // Email dispatch is part of the core in-app notification path, so it runs
        // synchronously here rather than only via the optional Kafka consumer - see
        // README "In-app notifications". Wrapped so a dispatch hiccup never breaks
        // the claim action that triggered it.
        try {
            dispatchService.sendEmail(recipient.getEmail(), "Chubb Claims Update — Claim #" + claim.getId(), message, "IN_APP");
        } catch (Exception e) {
            log.warn("Email dispatch failed for claim {} recipient {}: {}", claim.getId(), recipient.getId(), e.getMessage());
        }
    }

    /** Fans out to every user currently in the MANAGER role - no per-market routing yet, see README. */
    public void notifyManagers(Claim claim, String message) {
        for (User manager : userRepository.findByRole(UserRole.MANAGER)) {
            notify(manager, claim, message);
        }
    }

    public List<NotificationDto> myNotifications(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user).stream()
                .map(NotificationDto::from).toList();
    }

    public long unreadCount(User user) {
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    public void markRead(User user, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getRecipient().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        n.setRead(true);
    }

    public void markAllRead(User user) {
        notificationRepository.findByRecipientOrderByCreatedAtDesc(user).forEach(n -> n.setRead(true));
    }
}
