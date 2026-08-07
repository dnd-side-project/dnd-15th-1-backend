package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.notification.application.PushDeviceService;
import kr.omong.dulpick.domain.notification.application.RegisterPushDeviceCommand;
import kr.omong.dulpick.domain.notification.application.exception.PushRegistrationUnavailableException;
import kr.omong.dulpick.domain.notification.domain.PushDeviceRepository;
import kr.omong.dulpick.domain.notification.domain.PushPlatform;
import kr.omong.dulpick.domain.notification.domain.PushProviderType;
import kr.omong.dulpick.domain.notification.infrastructure.PushRegistrationCipher;
import kr.omong.dulpick.domain.notification.infrastructure.PushRegistrationEncryptionException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PushDeviceServiceTest {

    @Test
    void translatesEncryptionFailureToStableBusinessError() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        PushDeviceRepository pushDeviceRepository = mock(PushDeviceRepository.class);
        PushRegistrationCipher registrationCipher = mock(PushRegistrationCipher.class);
        Member member = Member.create(Instant.EPOCH);
        when(memberRepository.findForUpdateById(1L)).thenReturn(Optional.of(member));
        when(registrationCipher.encrypt("fcm-token"))
                .thenThrow(new PushRegistrationEncryptionException());
        PushDeviceService service = new PushDeviceService(
                memberRepository,
                pushDeviceRepository,
                registrationCipher,
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> service.register(1L, new RegisterPushDeviceCommand(
                UUID.randomUUID(),
                PushPlatform.IOS,
                PushProviderType.FCM,
                "fcm-token",
                "1.0.0"
        ))).isInstanceOf(PushRegistrationUnavailableException.class)
                .hasMessage("푸시 디바이스를 등록할 수 없습니다. 잠시 후 다시 시도해 주세요");
    }
}
