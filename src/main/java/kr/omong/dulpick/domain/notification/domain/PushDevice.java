package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;

import java.time.Instant;

@Entity
@Table(name = "push_devices")
public class PushDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "device_id", nullable = false, length = 36)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PushPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PushProviderType provider;

    @Column(name = "registration_hash", nullable = false, length = 64)
    private String registrationHash;

    @Column(name = "encrypted_registration_id", nullable = false, length = 2048)
    private String encryptedRegistrationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PushDeviceStatus status;

    @Column(name = "app_version", length = 30)
    private String appVersion;

    @Column(name = "last_registered_at", nullable = false)
    private Instant lastRegisteredAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PushDevice() {
    }

    private PushDevice(
            Member member,
            String deviceId,
            PushPlatform platform,
            PushProviderType provider,
            String registrationHash,
            String encryptedRegistrationId,
            String appVersion,
            Instant registeredAt
    ) {
        this.member = member;
        this.deviceId = deviceId;
        this.platform = platform;
        this.provider = provider;
        this.registrationHash = registrationHash;
        this.encryptedRegistrationId = encryptedRegistrationId;
        this.status = PushDeviceStatus.ACTIVE;
        this.appVersion = appVersion;
        this.lastRegisteredAt = registeredAt;
        this.createdAt = registeredAt;
        this.updatedAt = registeredAt;
    }

    public static PushDevice register(
            Member member,
            String deviceId,
            PushPlatform platform,
            PushProviderType provider,
            String registrationHash,
            String encryptedRegistrationId,
            String appVersion,
            Instant registeredAt
    ) {
        validateActive(member);
        return new PushDevice(
                member,
                deviceId,
                platform,
                provider,
                registrationHash,
                encryptedRegistrationId,
                appVersion,
                registeredAt
        );
    }

    public void refresh(
            Member member,
            String deviceId,
            PushPlatform platform,
            String registrationHash,
            String encryptedRegistrationId,
            String appVersion,
            Instant registeredAt
    ) {
        validateActive(member);
        this.member = member;
        this.deviceId = deviceId;
        this.platform = platform;
        this.registrationHash = registrationHash;
        this.encryptedRegistrationId = encryptedRegistrationId;
        this.status = PushDeviceStatus.ACTIVE;
        this.appVersion = appVersion;
        this.lastRegisteredAt = registeredAt;
        this.invalidatedAt = null;
        this.updatedAt = registeredAt;
    }

    public void logout(Instant loggedOutAt) {
        if (status != PushDeviceStatus.ACTIVE) {
            return;
        }
        status = PushDeviceStatus.LOGGED_OUT;
        updatedAt = loggedOutAt;
    }

    public void withdraw(Instant withdrawnAt) {
        status = PushDeviceStatus.WITHDRAWN;
        updatedAt = withdrawnAt;
    }

    public void invalidate(Instant invalidatedAt) {
        status = PushDeviceStatus.INVALIDATED;
        this.invalidatedAt = invalidatedAt;
        updatedAt = invalidatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return member.getId();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public PushProviderType getProvider() {
        return provider;
    }

    public PushDeviceStatus getStatus() {
        return status;
    }

    public String getEncryptedRegistrationId() {
        return encryptedRegistrationId;
    }

    public Instant getLastRegisteredAt() {
        return lastRegisteredAt;
    }

    private static void validateActive(Member member) {
        if (member == null || !member.isActive()) {
            throw new MemberNotActiveException();
        }
    }
}
