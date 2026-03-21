package com.nuv.tfgrs.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductoController {

    // TODO: inject ProductService in Phase 2

    @GetMapping
    public ResponseEntity<?> getAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        // TODO: implement in Phase 2
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        // TODO: implement in Phase 2
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Object request) {
        // TODO: implement in Phase 2
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Object request) {
        // TODO: implement in Phase 2
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // TODO: implement in Phase 2
        return ResponseEntity.noContent().build();
    }
}