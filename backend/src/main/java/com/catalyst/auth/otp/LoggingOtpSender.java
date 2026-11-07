package com.catalyst.auth.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/** Development fallback when no SMTP server is configured: prints the code to the server log. Never used when SMTP_HOST is set. */
class LoggingOtpSender implements OtpSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(String email, String name, String code, OtpPurpose purpose, Duration validFor) {
        log.warn("SMTP is not configured. {} code for {} is {} (valid {} min)", purpose, email, code, validFor.toMinutes());
    }
}
