package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.request.CrearPedidoProveedorRequest;
import com.nuv.tfgrs.dto.response.PedidoProveedorResponse;
import com.nuv.tfgrs.service.PedidoProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PedidoProveedorController {

    private final PedidoProveedorService pedidoProveedorService;

    @GetMapping("/api/proveedores/{providerId}/pedidos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PedidoProveedorResponse>> listarPorProveedor(@PathVariable Long providerId) {
        return ResponseEntity.ok(pedidoProveedorService.listarPorProveedor(providerId));
    }

    @PostMapping("/api/proveedores/{providerId}/pedidos")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PedidoProveedorResponse> crearBorrador(
            @PathVariable Long providerId,
            @Valid @RequestBody CrearPedidoProveedorRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoProveedorService.crearBorrador(providerId, request));
    }

    @PatchMapping("/api/pedidos/{orderId}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PedidoProveedorResponse> enviar(@PathVariable Long orderId) {
        return ResponseEntity.ok(pedidoProveedorService.enviar(orderId));
    }

    @PatchMapping("/api/pedidos/{orderId}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PedidoProveedorResponse> recibir(@PathVariable Long orderId) {
        return ResponseEntity.ok(pedidoProveedorService.recibir(orderId));
    }

    @PatchMapping("/api/pedidos/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PedidoProveedorResponse> cancelar(@PathVariable Long orderId) {
        return ResponseEntity.ok(pedidoProveedorService.cancelar(orderId));
    }
}
