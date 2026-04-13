package com.group10.cinemabooking.services;

import java.util.Map;

public interface MailService {
    void sendTemplateEmail(String to, String subject, String templateName, Map<String,Object> variables);
}
