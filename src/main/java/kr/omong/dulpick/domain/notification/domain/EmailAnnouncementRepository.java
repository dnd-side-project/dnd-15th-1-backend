package kr.omong.dulpick.domain.notification.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailAnnouncementRepository extends JpaRepository<EmailAnnouncement, UUID> {

    List<EmailAnnouncement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
