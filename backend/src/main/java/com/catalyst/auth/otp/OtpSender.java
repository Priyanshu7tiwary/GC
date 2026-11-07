package com.catalyst.auth.otp;

import java.time.Duration;

/** Delivers a one-time code to the user. */
public interface OtpSender {
    void send(String email, String name, String code, OtpPurpose purpose, Duration validFor);
}
