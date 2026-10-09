package com.tiki.chat.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asks shop-service which shop a seller owns (shop ids are NOT user ids). Positive answers are cached for a few
 * minutes so a chat session does not hit shop-service on every frame; any failure means "not the owner".
 */
@Slf4j
public class ShopOwnershipLookup implements StompJwtChannelInterceptor.ShopOwnership {

    private record Cached(Long shopId, long expiresAtMs) {}

    private static final long TTL_MS = Duration.ofMinutes(5).toMillis();

    private final String shopServiceUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    public ShopOwnershipLookup(String shopServiceUrl) {
        this.shopServiceUrl = shopServiceUrl.endsWith("/") ? shopServiceUrl.substring(0, shopServiceUrl.length() - 1) : shopServiceUrl;
    }

    @Override
    public boolean owns(StompJwtChannelInterceptor.StompUser user, Long shopId) {
        if (user == null || shopId == null) {
            return false;
        }
        if (user.role() != null && user.role().toUpperCase().contains("ADMIN")) {
            return true;
        }
        try {
            long userId = Long.parseLong(user.name());
            Cached cached = cache.get(userId);
            if (cached != null && cached.expiresAtMs() > System.currentTimeMillis()) {
                return shopId.equals(cached.shopId());
            }
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create(shopServiceUrl + "/api/v1/shops/seller/" + userId))
                            .timeout(Duration.ofSeconds(3)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }
            JsonNode id = mapper.readTree(response.body()).get("id");
            if (id == null || !id.canConvertToLong()) {
                return false;
            }
            cache.put(userId, new Cached(id.asLong(), System.currentTimeMillis() + TTL_MS));
            return shopId.equals(id.asLong());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Could not verify shop ownership for user {}: {}", user.name(), e.getMessage());
            return false;
        }
    }
}
