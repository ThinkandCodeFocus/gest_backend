package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.UserAccount;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class TransactionalEmailService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalEmailService.class);

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final String brevoApiKey;
    private final String brevoFromEmail;
    private final String brevoFromName;
    private final String appLoginUrl;

    public TransactionalEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@transport.local}") String senderEmail,
            @Value("${BREVO_API_KEY:}") String brevoApiKey,
            @Value("${MAIL_FROM_EMAIL:}") String brevoFromEmail,
            @Value("${MAIL_FROM_NAME:KIL Services}") String brevoFromName,
            @Value("${APP_LOGIN_URL:https://erpkilservices.com/}") String appLoginUrl
    ) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
        this.brevoApiKey = brevoApiKey;
        this.brevoFromEmail = brevoFromEmail;
        this.brevoFromName = brevoFromName;
        this.appLoginUrl = appLoginUrl;
    }

    public void sendPasswordResetEmail(UserAccount user, String token) {
        String subject = "Confirmation de changement de mot de passe";
        String body = "Bonjour " + user.getFullName() + ",\n\n"
                + "Un changement de mot de passe a ete demande pour votre compte KIL Services.\n\n"
                + "Code de confirmation : " + token + "\n\n"
                + "Utilisez ce code sur la page de connexion ou dans votre profil pour definir un nouveau mot de passe.\n"
                + "Lien de connexion : " + appLoginUrl;

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (Exception ex) {
            logger.warn("SMTP email failed for {}, trying Brevo API", user.getEmail(), ex);
            sendBrevoEmail(user, subject, body);
        }
    }

    private void sendBrevoEmail(UserAccount user, String subject, String body) {
        if (brevoApiKey == null || brevoApiKey.isBlank() || brevoFromEmail == null || brevoFromEmail.isBlank()) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "La configuration email n'est pas disponible.");
        }

        String payload = "{"
                + "\"sender\":{\"name\":\"" + escapeJson(brevoFromName) + "\",\"email\":\"" + escapeJson(brevoFromEmail) + "\"},"
                + "\"to\":[{\"email\":\"" + escapeJson(user.getEmail()) + "\",\"name\":\"" + escapeJson(user.getFullName()) + "\"}],"
                + "\"subject\":\"" + escapeJson(subject) + "\","
                + "\"textContent\":\"" + escapeJson(body) + "\""
                + "}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", brevoApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d'envoyer l'email de confirmation.");
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d'envoyer l'email de confirmation.");
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
