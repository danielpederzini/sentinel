package org.pdzsoftware.riskactionhandler.infrastructure.outbound.client;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.domain.exception.EmailClientException;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.EmailClientProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(destinationEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(mimeMessage);
        } catch (MailException | MessagingException exception) {
            throw new EmailClientException(String.format(
                    "Failed to send email with ID %s to %s", id, destinationEmail), exception);
        }
    }
}
