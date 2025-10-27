package com.tiki.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController("commonHealthController")
@RequestMapping("/api/v1/common")
public class HealthController {
	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> health() {
		Map<String, Object> body = new HashMap<>();
		body.put("status", "UP");
		body.put("service", "Common Service");
		body.put("timestamp", LocalDateTime.now());
		return ResponseEntity.ok(body);
	}
}
