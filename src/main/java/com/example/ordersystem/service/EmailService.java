package com.example.ordersystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.from:noreply@example.com}")
    private String fromEmail;

    @Async("taskExecutor")
    public void sendOrderConfirmationEmail(String toEmail, String orderNo) {
        log.info("开始发送订单确认邮件: toEmail={}, orderNo={}", toEmail, orderNo);
        
        if (javaMailSender == null) {
            log.warn("JavaMailSender未配置，模拟邮件发送，故意抛出异常测试异常处理");
            throw new RuntimeException("JavaMailSender not configured - 模拟邮件发送失败用于测试");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("订单确认 - " + orderNo);
            message.setText("尊敬的用户，\n\n您的订单 " + orderNo + " 已创建成功！\n\n感谢您的购买！");

            javaMailSender.send(message);
            log.info("订单确认邮件发送成功: toEmail={}, orderNo={}", toEmail, orderNo);
        } catch (MailException e) {
            log.error("订单确认邮件发送失败: toEmail={}, orderNo={}, error={}", 
                    toEmail, orderNo, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("订单确认邮件发送异常: toEmail={}, orderNo={}, error={}", 
                    toEmail, orderNo, e.getMessage(), e);
            throw new RuntimeException("邮件发送异常: " + e.getMessage(), e);
        }
    }

    @Async("taskExecutor")
    public void sendOrderStatusEmail(String toEmail, String orderNo, String status) {
        log.info("开始发送订单状态邮件: toEmail={}, orderNo={}, status={}", toEmail, orderNo, status);
        
        if (javaMailSender == null) {
            log.warn("JavaMailSender未配置，模拟邮件发送，故意抛出异常测试异常处理");
            throw new RuntimeException("JavaMailSender not configured - 模拟邮件发送失败用于测试");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("订单状态更新 - " + orderNo);
            message.setText("尊敬的用户，\n\n您的订单 " + orderNo + " 状态已更新为：" + status + "\n\n感谢您的关注！");

            javaMailSender.send(message);
            log.info("订单状态邮件发送成功: toEmail={}, orderNo={}, status={}", toEmail, orderNo, status);
        } catch (MailException e) {
            log.error("订单状态邮件发送失败: toEmail={}, orderNo={}, status={}, error={}", 
                    toEmail, orderNo, status, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("订单状态邮件发送异常: toEmail={}, orderNo={}, status={}, error={}", 
                    toEmail, orderNo, status, e.getMessage(), e);
            throw new RuntimeException("邮件发送异常: " + e.getMessage(), e);
        }
    }
}
