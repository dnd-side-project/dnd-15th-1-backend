package kr.omong.dulpick.domain.notification.application;

import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.domain.notification.domain.PushDevice;
import kr.omong.dulpick.domain.notification.domain.PushDeviceRepository;
import kr.omong.dulpick.domain.notification.domain.PushProviderType;
import kr.omong.dulpick.domain.notification.infrastructure.PushRegistrationCipher;
import kr.omong.dulpick.global.exception.ErrorCode;
import kr.omong.dulpick.global.exception.FieldValidationException;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PushDeviceService {

    private final MemberRepository memberRepository;
    private final PushDeviceRepository pushDeviceRepository;
    private final PushRegistrationCipher registrationCipher;
    private final Clock clock;

    public PushDeviceService(
            MemberRepository memberRepository,
            PushDeviceRepository pushDeviceRepository,
            PushRegistrationCipher registrationCipher,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.pushDeviceRepository = pushDeviceRepository;
        this.registrationCipher = registrationCipher;
        this.clock = clock;
    }

    @Transactional
    public PushDeviceView register(Long memberId, RegisterPushDeviceCommand command) {
        validateProvider(command.provider());
        Member member = lockActiveMember(memberId);
        String deviceId = command.deviceId().toString();
        String registrationHash = Sha256.hex(command.providerRegistrationId());
        Instant registeredAt = clock.instant();
        String encryptedRegistrationId = registrationCipher.encrypt(
                command.providerRegistrationId()
        );
        PushDevice device = findRegistrationTarget(command, deviceId, registrationHash)
                .map(existing -> refresh(
                        existing,
                        member,
                        command,
                        deviceId,
                        registrationHash,
                        encryptedRegistrationId,
                        registeredAt
                )).orElseGet(() -> register(
                        member,
                        command,
                        deviceId,
                        registrationHash,
                        encryptedRegistrationId,
                        registeredAt
                ));
        return save(device);
    }

    @Transactional
    public void unregister(Long memberId, UUID deviceId) {
        PushDevice device = pushDeviceRepository.findForUpdateByProviderAndDeviceId(
                PushProviderType.FCM,
                deviceId.toString()
        ).filter(candidate -> candidate.getMemberId().equals(memberId))
                .orElseThrow(PushDeviceNotFoundException::new);
        device.logout(clock.instant());
    }

    @Transactional
    public void disableAllForWithdrawal(Long memberId, Instant withdrawnAt) {
        pushDeviceRepository.findAllForUpdateByMemberId(memberId)
                .forEach(device -> device.withdraw(withdrawnAt));
    }

    private Member lockActiveMember(Long memberId) {
        Member member = memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
        return member;
    }

    private Optional<PushDevice> findRegistrationTarget(
            RegisterPushDeviceCommand command,
            String deviceId,
            String registrationHash
    ) {
        Optional<PushDevice> tokenDevice = pushDeviceRepository
                .findForUpdateByProviderAndRegistrationHash(
                        command.provider(),
                        registrationHash
                );
        Optional<PushDevice> installedDevice = pushDeviceRepository
                .findForUpdateByProviderAndDeviceId(command.provider(), deviceId);
        if (tokenDevice.isPresent() && installedDevice.isPresent()
                && !tokenDevice.get().getId().equals(installedDevice.get().getId())) {
            pushDeviceRepository.delete(tokenDevice.get());
            pushDeviceRepository.flush();
        }
        return installedDevice.isPresent() ? installedDevice : tokenDevice;
    }

    private PushDevice refresh(
            PushDevice device,
            Member member,
            RegisterPushDeviceCommand command,
            String deviceId,
            String registrationHash,
            String encryptedRegistrationId,
            Instant registeredAt
    ) {
        device.refresh(
                member,
                deviceId,
                command.platform(),
                registrationHash,
                encryptedRegistrationId,
                command.appVersion(),
                registeredAt
        );
        return device;
    }

    private PushDevice register(
            Member member,
            RegisterPushDeviceCommand command,
            String deviceId,
            String registrationHash,
            String encryptedRegistrationId,
            Instant registeredAt
    ) {
        return PushDevice.register(
                member,
                deviceId,
                command.platform(),
                command.provider(),
                registrationHash,
                encryptedRegistrationId,
                command.appVersion(),
                registeredAt
        );
    }

    private PushDeviceView save(PushDevice device) {
        try {
            return toView(pushDeviceRepository.saveAndFlush(device));
        } catch (DataIntegrityViolationException exception) {
            throw new PushDeviceConflictException(exception);
        }
    }

    private void validateProvider(PushProviderType provider) {
        if (provider == PushProviderType.FCM) {
            return;
        }
        throw new FieldValidationException(
                ErrorCode.INVALID_INPUT,
                "provider",
                "unsupported",
                "현재 지원하는 푸시 공급자는 FCM입니다"
        );
    }

    private PushDeviceView toView(PushDevice device) {
        return new PushDeviceView(
                UUID.fromString(device.getDeviceId()),
                device.getStatus(),
                device.getLastRegisteredAt()
        );
    }
}
