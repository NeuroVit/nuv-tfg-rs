package com.nuv.tfgrs.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailDispatchService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final RestClient restClient = RestClient.create();

    @Value("${EMAIL_PROVIDER:smtp}")
    private String emailProvider;

    @Value("${spring.mail.username:}")
    private String smtpFromEmail;

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${EMAIL_FROM:}")
    private String resendFromEmail;

    public EmailDispatchService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendEmail(String fromName, String to, String subject, String textBody, String htmlBody) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("El destinatario del correo es obligatorio");
        }

        if ("resend".equalsIgnoreCase(emailProvider)) {
            sendWithResend(to.trim(), subject, textBody, htmlBody);
            return;
        }

        sendWithSmtp(fromName, to.trim(), subject, textBody, htmlBody);
    }

    private void sendWithSmtp(String fromName, String to, String subject, String textBody, String htmlBody) {
        if (smtpFromEmail == null || smtpFromEmail.isBlank()) {
            throw new IllegalStateException("spring.mail.username no configurado");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender no disponible");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(smtpFromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);
            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo enviar correo por SMTP", ex);
        }
    }

    private void sendWithResend(String to, String subject, String textBody, String htmlBody) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY no configurado");
        }

        String from = (resendFromEmail != null && !resendFromEmail.isBlank())
                ? resendFromEmail
                : "NeuralStock <onboarding@resend.dev>";

        Map<String, Object> payload = Map.of(
                "from", from,
                "to", List.of(to),
                "subject", subject,
                "text", textBody,
                "html", htmlBody
        );

        restClient.post()
                .uri("https://api.resend.com/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + resendApiKey)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        log.debug("Correo enviado por Resend a {}", to);
    }
}

