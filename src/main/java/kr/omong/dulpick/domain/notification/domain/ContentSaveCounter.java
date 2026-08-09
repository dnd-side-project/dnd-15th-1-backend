package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@IdClass(ContentSaveCounterId.class)
@Table(name = "couple_content_save_counters")
public class ContentSaveCounter {

    private static final long MILESTONE_INTERVAL = 10L;

    @Id
    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Id
    @Column(name = "saver_member_id", nullable = false)
    private Long saverMemberId;

    @Column(name = "save_count", nullable = false)
    private long saveCount;

    @Column(name = "last_notified_milestone", nullable = false)
    private long lastNotifiedMilestone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContentSaveCounter() {
    }

    public boolean reachNewMilestone() {
        return saveCount % MILESTONE_INTERVAL == 0
                && saveCount > lastNotifiedMilestone;
    }

    public void markNotified() {
        if (!reachNewMilestone()) {
            return;
        }
        lastNotifiedMilestone = saveCount;
    }

    public long getSaveCount() {
        return saveCount;
    }
}
