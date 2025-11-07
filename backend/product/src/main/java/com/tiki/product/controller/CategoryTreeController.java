package com.tiki.product.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Category Tree Controller
 * Provides category hierarchy and tree structure
 */
@RestController
@RequestMapping("/api/v1/categories")
@Slf4j
public class CategoryTreeController {
    
    /**
     * Get category tree
     * GET /api/v1/categories/tree
     */
    @GetMapping("/tree")
    public ResponseEntity<?> getCategoryTree() {
        log.info("Getting category tree");
        
        // Return mock category tree
        List<Map<String, Object>> tree = List.of(
            Map.of(
                "id", 1,
                "name", "Điện Tử",
                "children", List.of(
                    Map.of("id", 11, "name", "Điện Thoại"),
                    Map.of("id", 12, "name", "Laptop")
                )
            ),
            Map.of(
                "id", 2,
                "name", "Thời Trang",
                "children", List.of(
                    Map.of("id", 21, "name", "Quần Áo Nam"),
                    Map.of("id", 22, "name", "Quần Áo Nữ")
                )
            )
        );
        
        return ResponseEntity.ok(Map.of(
            "tree", tree,
            "total", tree.size()
        ));
    }
}
