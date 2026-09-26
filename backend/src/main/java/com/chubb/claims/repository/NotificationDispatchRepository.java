package com.chubb.claims.repository;

import com.chubb.claims.domain.NotificationDispatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, Long> {
    List<NotificationDispatch> findAllByOrderByDispatchedAtDesc();
}
