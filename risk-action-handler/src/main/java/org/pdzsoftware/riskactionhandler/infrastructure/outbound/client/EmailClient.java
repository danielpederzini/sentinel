package org.pdzsoftware.riskactionhandler.infrastructure.outbound.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.domain.exception.EmailClientException;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.EmailClientProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailClient {
    private final JavaMailSender mailSender;
    private final EmailClientProperties emailClientProperties;

    public void sendEmail(String id,
                          String subject,
                          String content) {
        String destinationEmail = emailClientProperties.getDestinationEmail();

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinationEmail);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
        } catch (MailException exception) {
            throw new EmailClientException(String.format(
                    "Failed to send email with ID %s to %s", id, destinationEmail), exception);
        }
    }
}
