package com.admin.service.impl;

import com.admin.config.AdminMailProperties;
import com.admin.exception.TenantEmailDeliveryException;
import com.admin.service.TenantCredentialEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;



@Service
public class SmtpTenantCredentialEmailService implements TenantCredentialEmailService {
    private static final Logger log =
            (Logger) LoggerFactory.getLogger(SmtpTenantCredentialEmailService.class);
    private final JavaMailSender mailSender;
    private final AdminMailProperties properties;

    public SmtpTenantCredentialEmailService(
            JavaMailSender mailSender,
            AdminMailProperties properties
    ) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendTemporaryPassword(
            String recipientEmail,
            String recipientName,
            String tenantName,
            String tenantCode,
            String temporaryPassword
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(recipientEmail);
        message.setSubject("[SmartHub] Thông tin đăng nhập tenant " + tenantCode);
        message.setText("""
                Xin chào %s,

                Tài khoản quản lý tenant của bạn đã được tạo.

                Tenant: %s
                Mã tenant: %s
                Email đăng nhập: %s
                Mật khẩu tạm thời: %s
                Đăng nhập tại: %s

                Vì lý do bảo mật, vui lòng đăng nhập và đổi mật khẩu ngay lần đầu sử dụng.
                Không chia sẻ email này hoặc mật khẩu tạm thời với người khác.

                SmartHub
                """.formatted(
                recipientName,
                tenantName,
                tenantCode,
                recipientEmail,
                temporaryPassword,
                properties.getLoginUrl()));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error(
                    "Failed to send tenant credential email via configured SMTP server: {}",
                    exception.getMessage(),
                    exception
            );

            throw new TenantEmailDeliveryException(
                    "Không thể gửi mật khẩu đến email này. Vui lòng kiểm tra địa chỉ email hoặc cấu hình máy chủ gửi thư.",
                    exception
            );
        }
    }
}
