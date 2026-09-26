package com.chubb.claims.repository;

import com.chubb.claims.domain.Notification;
import com.chubb.claims.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndReadFalse(User recipient);
}
