package com.catalyst.auth.otp;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
class OtpSenderConfig {
    /** Email when Spring Boot configured a mail sender (SMTP_HOST set); otherwise the log-only fallback. */
    @Bean
    OtpSender otpSender(ObjectProvider<JavaMailSender> mail, @Value("${app.mail.from:CATalyst <no-reply@catalyst.local>}") String from) {
        JavaMailSender sender = mail.getIfAvailable();
        return sender != null ? new EmailOtpSender(sender, from) : new LoggingOtpSender();
    }
}
