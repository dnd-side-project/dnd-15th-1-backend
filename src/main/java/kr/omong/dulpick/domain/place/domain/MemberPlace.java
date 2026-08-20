package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "member_places")
public class MemberPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "source_import_id")
    private Long sourceImportId;

    @Column(length = 100)
    private String alias;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    protected MemberPlace() {
    }

    private MemberPlace(
            Long memberId,
            Place place,
            Long sourceImportId,
            String alias,
            Instant savedAt
    ) {
        this.memberId = memberId;
        this.place = place;
        this.sourceImportId = sourceImportId;
        this.alias = alias;
        this.savedAt = savedAt;
    }

    public static MemberPlace save(
            Long memberId,
            Place place,
            Long sourceImportId,
            String alias,
            Instant savedAt
    ) {
        return new MemberPlace(
                memberId,
                place,
                sourceImportId,
                alias,
                savedAt
        );
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Place getPlace() {
        return place;
    }

    public void updateAlias(String alias) {
        this.alias = alias == null || alias.isBlank() ? null : alias.strip();
    }

    public String getAlias() {
        return alias;
    }

    public Instant getSavedAt() {
        return savedAt;
    }
}
