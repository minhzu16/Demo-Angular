package com.tiki.shop.service;

import com.tiki.common.entity.SellerApplication;
import com.tiki.common.repository.SellerApplicationRepository;
import com.tiki.shop.entity.ShopEntity;
import com.tiki.shop.client.AuthClient;
import com.tiki.shop.config.ShopRabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerApplicationService {

    private final SellerApplicationRepository repository;
    private final AuthClient authClient;
    private final ShopService shopService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public SellerApplication createApplication(Long userId, SellerApplication application) {
        if (repository.existsByUserIdAndStatus(userId, SellerApplication.Status.PENDING)) {
            throw new RuntimeException("You already have a pending seller application");
        }
        application.setUserId(userId);
        application.setStatus(SellerApplication.Status.PENDING);
        return repository.save(application);
    }

    public List<SellerApplication> getApplicationsByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public List<SellerApplication> getAllByStatus(String status) {
        if (status == null || status.isBlank()) {
            return repository.findAll();
        }
        return repository.findByStatus(SellerApplication.Status.valueOf(status.toUpperCase()));
    }

    @Transactional
    public SellerApplication approveApplication(Long id, Long adminId) {
        SellerApplication application = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (application.getStatus() != SellerApplication.Status.PENDING) {
            throw new RuntimeException("Application is already processed");
        }

        application.setStatus(SellerApplication.Status.APPROVED);
        application.setReviewedBy(adminId);
        application.setReviewedAt(LocalDateTime.now());
        
        SellerApplication savedApp = repository.save(application);

        // Update user role to SELLER
        try {
            authClient.updateUserRole(application.getUserId(), "SELLER");
        } catch (Exception e) {
            log.error("Failed to update user role to SELLER for userId: {}", application.getUserId(), e);
            throw new RuntimeException("Failed to update user role. Please try again.");
        }

        // Create Shop
        ShopEntity shop = ShopEntity.builder()
                .sellerId(application.getUserId())
                .name(application.getShopName())
                .description(application.getShopDescription())
                .address(application.getAddress())
                .isActive(true)
                .build();
        shopService.createOrUpdateShop(shop);

        // Send notification
        sendNotification(application.getUserId(), "Seller Application Approved", 
            "Congratulations! Your shop '" + application.getShopName() + "' has been approved. You are now a SELLER.");

        return savedApp;
    }

    @Transactional
    public SellerApplication rejectApplication(Long id, Long adminId, String reason) {
        SellerApplication application = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (application.getStatus() != SellerApplication.Status.PENDING) {
            throw new RuntimeException("Application is already processed");
        }

        application.setStatus(SellerApplication.Status.REJECTED);
        application.setRejectionReason(reason);
        application.setReviewedBy(adminId);
        application.setReviewedAt(LocalDateTime.now());
        
        SellerApplication savedApp = repository.save(application);

        // Send notification
        sendNotification(application.getUserId(), "Seller Application Rejected", 
            "We're sorry, but your shop application was rejected. Reason: " + reason);

        return savedApp;
    }

    private void sendNotification(Long userId, String title, String message) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("title", title);
            event.put("message", message);
            event.put("type", "SYSTEM");
            event.put("link", "/seller/dashboard");
            
            rabbitTemplate.convertAndSend(ShopRabbitMQConfig.NOTIFICATION_EXCHANGE, ShopRabbitMQConfig.NOTIFICATION_ROUTING_KEY, event);
            log.info("Sent notification event for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send notification to user: {}", userId, e);
        }
    }
}
