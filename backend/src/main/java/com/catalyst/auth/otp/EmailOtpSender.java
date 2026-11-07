package com.catalyst.auth.otp;

import com.catalyst.common.error.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;

/** Sends the code by SMTP (any provider: Mailpit locally, SES/SendGrid/Gmail SMTP in production). */
class EmailOtpSender implements OtpSender {
    private static final Logger log = LoggerFactory.getLogger(EmailOtpSender.class);

    private final JavaMailSender mail;
    private final String from;

    EmailOtpSender(JavaMailSender mail, String from) {
        this.mail = mail;
        this.from = from;
    }

    @Override
    public void send(String email, String name, String code, OtpPurpose purpose, Duration validFor) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(email);
        msg.setSubject(code + " is your CATalyst " + (purpose == OtpPurpose.SIGN_UP ? "verification" : "sign-in") + " code");
        msg.setText("""
                Hi %s,

                Your CATalyst %s code is:

                    %s

                It expires in %d minutes. If you didn't try to %s, ignore this email; your account stays safe without the code.
                """.formatted(name, purpose == OtpPurpose.SIGN_UP ? "verification" : "sign-in", code, validFor.toMinutes(),
                purpose == OtpPurpose.SIGN_UP ? "create an account" : "sign in"));
        try {
            mail.send(msg);
        } catch (MailException e) {
            log.error("Could not send OTP email", e);
            throw new ServiceUnavailableException("We couldn't send the code right now. Try again in a minute.");
        }
    }
}
