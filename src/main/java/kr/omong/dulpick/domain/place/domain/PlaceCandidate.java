package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "place_candidates")
public class PlaceCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_id", nullable = false)
    private Long importId;

    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "extracted_name", nullable = false, length = 255)
    private String extractedName;

    @Column(name = "extracted_address_hint", length = 500)
    private String extractedAddressHint;

    @Column(name = "evidence", length = 1_000)
    private String evidence;

    @Column(name = "mention_type", length = 40)
    private String mentionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private PlaceVerificationStatus verificationStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlaceCandidate() {
    }

    private PlaceCandidate(
            Long importId,
            Long placeId,
            String extractedName,
            String extractedAddressHint,
            String evidence,
            String mentionType,
            PlaceVerificationStatus verificationStatus,
            Instant createdAt
    ) {
        this.importId = importId;
        this.placeId = placeId;
        this.extractedName = extractedName;
        this.extractedAddressHint = extractedAddressHint;
        this.evidence = evidence;
        this.mentionType = mentionType;
        this.verificationStatus = verificationStatus;
        this.createdAt = createdAt;
    }

    public static PlaceCandidate verified(
            Long importId,
            Long placeId,
            String extractedName,
            String extractedAddressHint,
            String evidence,
            String mentionType,
            Instant createdAt
    ) {
        return new PlaceCandidate(
                importId,
                placeId,
                extractedName,
                extractedAddressHint,
                evidence,
                mentionType,
                PlaceVerificationStatus.VERIFIED,
                createdAt
        );
    }

    public Long getId() {
        return id;
    }

    public Long getImportId() {
        return importId;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public String getExtractedName() {
        return extractedName;
    }

    public String getExtractedAddressHint() {
        return extractedAddressHint;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getMentionType() {
        return mentionType;
    }

    public PlaceVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }
}
