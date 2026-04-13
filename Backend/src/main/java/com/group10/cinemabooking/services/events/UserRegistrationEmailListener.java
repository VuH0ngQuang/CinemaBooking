package com.group10.cinemabooking.services.events;

import com.group10.cinemabooking.configurations.AppConf;
import com.group10.cinemabooking.models.Users;
import com.group10.cinemabooking.repository.UserRepository;
import com.group10.cinemabooking.services.MailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserRegistrationEmailListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationEmailListener.class);

    private final UserRepository userRepository;
    private final MailService mailService;
    private final AppConf appConf;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserRegistered(UserRegistrationEmailEvent event) {
        if (event.userId() == null) {
            return;
        }
        try {
            sendWelcomeEmail(event.userId());
        } catch (Exception ex) {
            log.warn("Async registration email failed for user {}: {}", event.userId(), ex.getMessage());
        }
    }

    private void sendWelcomeEmail(Long userId) {
        Users user = userRepository.findActiveById(userId).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        Map<String, Object> mailVariables = new HashMap<>();
        mailVariables.put("username", user.getFull_name());
        mailVariables.put("loginUrl", appConf.getAppDomain() + "/login");

        mailService.sendTemplateEmail(
                user.getEmail(),
                "Welcome to Cinema Booking!",
                "registration-confirmation",
                mailVariables
        );
    }
}
