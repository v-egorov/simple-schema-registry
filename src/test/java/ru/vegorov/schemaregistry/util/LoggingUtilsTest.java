package ru.vegorov.schemaregistry.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LoggingUtilsTest {

    @Test
    void truncatePayload_shouldReturnOriginalWhenMaxLengthIsZero() {
        String payload = "This is a test payload";
        String result = LoggingUtils.truncatePayload(payload, 0);
        assertThat(result).isEqualTo(payload);
    }

    @Test
    void truncatePayload_shouldReturnOriginalWhenPayloadLengthIsLessThanMax() {
        String payload = "Short";
        String result = LoggingUtils.truncatePayload(payload, 10);
        assertThat(result).isEqualTo(payload);
    }

    @Test
    void truncatePayload_shouldTruncateWhenPayloadExceedsMaxLength() {
        String payload = "This is a very long payload that should be truncated for logging purposes";
        String result = LoggingUtils.truncatePayload(payload, 20);
        assertThat(result.length()).isEqualTo(20);
        assertThat(result).startsWith("This");
        assertThat(result).endsWith("ses");
        assertThat(result).contains("[truncated]");
    }

    @Test
    void truncatePayload_shouldHandleEdgeCaseWithSmallMaxLength() {
        String payload = "ABCDEFG";
        String result = LoggingUtils.truncatePayload(payload, 5);
        // since truncatedMessage length 11 > 5, return first 5 chars
        assertThat(result).isEqualTo("ABCDE");
        assertThat(result.length()).isEqualTo(5);
    }

    @Test
    void truncatePayload_shouldHandleEmptyString() {
        String payload = "";
        String result = LoggingUtils.truncatePayload(payload, 10);
        assertThat(result).isEqualTo("");
    }

    @Test
    void truncatePayload_shouldHandleMaxLengthEqualToPayloadLength() {
        String payload = "Exact";
        String result = LoggingUtils.truncatePayload(payload, 5);
        assertThat(result).isEqualTo("Exact");
    }
}