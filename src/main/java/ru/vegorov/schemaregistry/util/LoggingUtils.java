package ru.vegorov.schemaregistry.util;

/**
 * Utility class for logging-related operations.
 */
public class LoggingUtils {

    /**
     * Truncate payload string if it exceeds max length.
     * Keeps beginning and end, cuts middle with truncated message.
     */
    public static String truncatePayload(String payload, int maxLength) {
        if (maxLength <= 0 || payload.length() <= maxLength) {
            return payload;
        }
        String truncatedMessage = "[truncated]";
        int ellipsisLength = truncatedMessage.length();
        int availableLength = maxLength - ellipsisLength;
        if (availableLength <= 0) {
            return payload.substring(0, maxLength);
        }
        int firstHalf = (availableLength + 1) / 2;
        int secondHalf = availableLength / 2;
        return payload.substring(0, firstHalf) + truncatedMessage + payload.substring(payload.length() - secondHalf);
    }
}
