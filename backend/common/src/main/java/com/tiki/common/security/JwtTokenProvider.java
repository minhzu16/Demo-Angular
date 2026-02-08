package com.tiki.common.security;

/**
 * Minimal placeholder JWT token provider used by downstream services.
 *
 * In the current architecture, the API Gateway is responsible for
 * authenticating JWT tokens and forwarding user identity via headers
 * (e.g. X-User-Id, X-Username). Downstream services only need a
 * lightweight type here to satisfy existing wiring, so this class
 * intentionally has no behaviour.
 */
public class JwtTokenProvider {
    // This class can be extended in the future if services
    // need to validate or generate tokens locally.
}
