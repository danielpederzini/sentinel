package org.pdzsoftware.riskactionhandler.infrastructure.outbound.client;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pdzsoftware.riskactionhandler.domain.exception.EmailClientException;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.EmailClientProperties;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.DESTINATION_EMAIL;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.EMAIL_ERROR_MESSAGE_PREFIX;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.EMAIL_SUBJECT_PREFIX;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.MAIL_SEND_FAILURE_MESSAGE;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.TRANSACTION_ID;

@ExtendWith(MockitoExtension.class)
class EmailClientTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailClientProperties emailClientProperties;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailClient emailClient;

    @Test
    void sendEmail_shouldSendMimeMessage_whenConfigured() {
        when(emailClientProperties.getDestinationEmail()).thenReturn(DESTINATION_EMAIL);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailClient.sendEmail(TRANSACTION_ID, EMAIL_SUBJECT_PREFIX + TRANSACTION_ID, "<p>alert</p>");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
        verify(emailClientProperties).getDestinationEmail();
    }

    @Test
    void sendEmail_shouldThrowEmailClientException_whenMailSenderFails() {
        when(emailClientProperties.getDestinationEmail()).thenReturn(DESTINATION_EMAIL);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException(MAIL_SEND_FAILURE_MESSAGE)).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailClient.sendEmail(
                TRANSACTION_ID,
                EMAIL_SUBJECT_PREFIX + TRANSACTION_ID,
                "<p>alert</p>"))
                .isInstanceOf(EmailClientException.class)
                .hasMessageContaining(EMAIL_ERROR_MESSAGE_PREFIX)
                .hasMessageContaining(TRANSACTION_ID)
                .hasMessageContaining(DESTINATION_EMAIL);
    }
}
