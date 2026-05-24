package com.hospital.ai.consult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AiConsultServiceTest {

    @Test
    void normalizeTitleDefaultsToNewConversationWhenBlank() {
        assertThat(AiConsultService.normalizeTitle(null)).isEqualTo("新对话");
        assertThat(AiConsultService.normalizeTitle("")).isEqualTo("新对话");
        assertThat(AiConsultService.normalizeTitle("   ")).isEqualTo("新对话");
    }

    @Test
    void normalizeTitleTrimsAndCapsLength() {
        String big = "x".repeat(300);
        String out = AiConsultService.normalizeTitle(big);
        assertThat(out).hasSize(AiConsultService.TITLE_MAX_LENGTH);
    }

    @Test
    void createSessionRejectsAnonymousCaller() {
        AiConsultService svc = new AiConsultService(null, null);
        assertThatThrownBy(() -> svc.createSession(null, "x"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
