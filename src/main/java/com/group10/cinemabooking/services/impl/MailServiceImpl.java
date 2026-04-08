package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.configurations.AppConf;
import com.group10.cinemabooking.services.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);
    private final AppConf appConf;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Override
    public void sendTemplateEmail(String to,
                                  String subject,
                                  String templateName,
                                  Map<String, Object> variables
    ) {
        Context context = new Context();
        if (variables != null) {
            variables.forEach(context::setVariable);
        }
        String html = templateEngine.process(templateName, context);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.setFrom(appConf.getMail().getUsername());
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email: {}",e.getMessage());
        }
    }
}
