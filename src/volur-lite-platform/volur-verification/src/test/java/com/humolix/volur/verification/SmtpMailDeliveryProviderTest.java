package com.humolix.volur.verification;

import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.mail.bo.MailMessageBO;
import com.humolix.volur.verification.mail.provider.SmtpMailDeliveryProvider;
import jakarta.mail.Address;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import static org.mockito.Mockito.when;

public class SmtpMailDeliveryProviderTest {

    private SystemSettingService systemSettingService;
    private SmtpMailDeliveryProvider provider;
    private JavaMailSenderImpl mailSender;

    @BeforeEach
    public void setup() {
        systemSettingService = Mockito.mock(SystemSettingService.class);
        provider = new SmtpMailDeliveryProvider(systemSettingService);

        mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.example.com");
        mailSender.setPort(465);
        mailSender.setUsername("service@volur.local");
        mailSender.setPassword("secret123");
    }

    @Test
    public void testApplySenderWithWhitespaceAndChineseDisplayNameDoesNotThrowException() throws Exception {
        when(systemSettingService.getSettingValue("system.mail.sender_name", "")).thenReturn("Volur Lite 安全演练平台");

        MailMessageBO message = MailMessageBO.builder()
                .from("service@volur.local")
                .senderName("Volur Lite 安全演练平台")
                .to("recipient@example.com")
                .subject("Test")
                .content("Content")
                .build();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

        Assertions.assertDoesNotThrow(() -> provider.applySender(helper, mailSender, message));

        Address[] fromAddresses = mimeMessage.getFrom();
        Assertions.assertNotNull(fromAddresses);
        Assertions.assertEquals(1, fromAddresses.length);

        InternetAddress inet = (InternetAddress) fromAddresses[0];
        Assertions.assertEquals("service@volur.local", inet.getAddress());
        Assertions.assertEquals("Volur Lite 安全演练平台", inet.getPersonal());
    }

    @Test
    public void testApplySenderHandlesLegacyNicknameInFromGracefully() throws Exception {
        when(systemSettingService.getSettingValue("system.mail.sender_name", "")).thenReturn("");
        when(systemSettingService.getSettingValue("system.mail.smtp_user", "noreply@volur.local")).thenReturn("service@volur.local");

        MailMessageBO message = MailMessageBO.builder()
                .from("Volur Lite Security Team") // Legacy bug where sender_name was stored in from
                .to("recipient@example.com")
                .subject("Test")
                .content("Content")
                .build();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

        // Should NOT throw "Local address contains control or whitespace"
        Assertions.assertDoesNotThrow(() -> provider.applySender(helper, mailSender, message));

        Address[] fromAddresses = mimeMessage.getFrom();
        Assertions.assertNotNull(fromAddresses);
        Assertions.assertEquals(1, fromAddresses.length);

        InternetAddress inet = (InternetAddress) fromAddresses[0];
        Assertions.assertEquals("service@volur.local", inet.getAddress());
        Assertions.assertEquals("Volur Lite Security Team", inet.getPersonal());
    }

    @Test
    public void testApplySenderWithAngleBracketsInFrom() throws Exception {
        MailMessageBO message = MailMessageBO.builder()
                .from("Volur Admin <admin@volur.local>")
                .to("recipient@example.com")
                .subject("Test")
                .content("Content")
                .build();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

        Assertions.assertDoesNotThrow(() -> provider.applySender(helper, mailSender, message));

        Address[] fromAddresses = mimeMessage.getFrom();
        Assertions.assertNotNull(fromAddresses);
        Assertions.assertEquals(1, fromAddresses.length);

        InternetAddress inet = (InternetAddress) fromAddresses[0];
        Assertions.assertEquals("admin@volur.local", inet.getAddress());
        Assertions.assertEquals("Volur Admin", inet.getPersonal());
    }

    @Test
    public void testApplySenderWithoutSenderName() throws Exception {
        when(systemSettingService.getSettingValue("system.mail.sender_name", "")).thenReturn("");

        MailMessageBO message = MailMessageBO.builder()
                .from("service@volur.local")
                .to("recipient@example.com")
                .subject("Test")
                .content("Content")
                .build();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

        Assertions.assertDoesNotThrow(() -> provider.applySender(helper, mailSender, message));

        Address[] fromAddresses = mimeMessage.getFrom();
        Assertions.assertNotNull(fromAddresses);
        Assertions.assertEquals(1, fromAddresses.length);

        InternetAddress inet = (InternetAddress) fromAddresses[0];
        Assertions.assertEquals("service@volur.local", inet.getAddress());
        Assertions.assertNull(inet.getPersonal());
    }
}
