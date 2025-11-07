package com.tiki.product.controller;

import com.tiki.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductExistsController {

    @Autowired
    private ProductRepository repo;

    @GetMapping("/{id}/exists")
    public ResponseEntity<Void> exists(@PathVariable Integer id){
        return repo.existsById(id) ? ResponseEntity.ok().build()
                                   : ResponseEntity.notFound().build();
    }
}
