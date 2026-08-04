package com.admin.service.impl;

import com.admin.config.AdminMailProperties;
import com.admin.exception.TenantEmailDeliveryException;
import com.admin.service.TenantCredentialEmailService;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpTenantCredentialEmailService implements TenantCredentialEmailService {

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
        message.setSubject("[OmnichannelPOS] Thông tin đăng nhập tenant " + tenantCode);
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

                OmnichannelPOS
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
            throw new TenantEmailDeliveryException(
                    "Không thể gửi mật khẩu đến email này. Vui lòng kiểm tra địa chỉ email hoặc cấu hình máy chủ gửi thư.",
                    exception);
        }
    }
}
