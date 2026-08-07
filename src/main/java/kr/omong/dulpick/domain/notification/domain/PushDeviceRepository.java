package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT device
            FROM PushDevice device
            WHERE device.provider = :provider
              AND device.deviceId = :deviceId
            """)
    Optional<PushDevice> findForUpdateByProviderAndDeviceId(
            @Param("provider") PushProviderType provider,
            @Param("deviceId") String deviceId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT device
            FROM PushDevice device
            WHERE device.provider = :provider
              AND device.registrationHash = :registrationHash
            """)
    Optional<PushDevice> findForUpdateByProviderAndRegistrationHash(
            @Param("provider") PushProviderType provider,
            @Param("registrationHash") String registrationHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT device
            FROM PushDevice device
            WHERE device.member.id = :memberId
            """)
    List<PushDevice> findAllForUpdateByMemberId(@Param("memberId") Long memberId);

    @Query("""
            SELECT device
            FROM PushDevice device
            WHERE device.member.id = :memberId
              AND device.status = :status
            """)
    List<PushDevice> findAllByMemberIdAndStatus(
            @Param("memberId") Long memberId,
            @Param("status") PushDeviceStatus status
    );
}
