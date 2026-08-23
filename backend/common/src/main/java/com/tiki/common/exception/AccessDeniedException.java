package com.tiki.common.exception;

/**
 * Simple runtime exception to represent access-denied violations across services.
 * Used by business logic (e.g. order-service) when a user attempts to access
 * a resource that does not belong to them.
 */
public class AccessDeniedException extends RuntimeException {

    private final Long userId;
    private final String resource;
    private final Object resourceId;

    public AccessDeniedException(Long userId, String resource, Object resourceId) {
        super(buildMessage(userId, resource, resourceId));
        this.userId = userId;
        this.resource = resource;
        this.resourceId = resourceId;
    }

    private static String buildMessage(Long userId, String resource, Object resourceId) {
        return String.format("User %s is not allowed to access %s %s", userId, resource, resourceId);
    }

    public Long getUserId() {
        return userId;
    }

    public String getResource() {
        return resource;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
