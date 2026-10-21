package com.nuv.tfgrs.features.pedidosproveedor.controller;

import com.nuv.tfgrs.features.pedidosproveedor.dto.request.CrearPedidoProveedorRequest;
import com.nuv.tfgrs.features.pedidosproveedor.dto.response.PedidoProveedorResponse;
import com.nuv.tfgrs.features.pedidosproveedor.service.PedidoProveedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PedidoProveedorController {

    private final PedidoProveedorService pedidoProveedorService;

    public PedidoProveedorController(PedidoProveedorService pedidoProveedorService) {
        this.pedidoProveedorService = pedidoProveedorService;
    }

    @GetMapping("/proveedores/{providerId}/pedidos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PedidoProveedorResponse>> listarPorProveedor(@PathVariable Long providerId) {
        return ResponseEntity.ok(pedidoProveedorService.listarPorProveedor(providerId));
    }

    @PostMapping("/proveedores/{providerId}/pedidos")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PedidoProveedorResponse> crearBorrador(
            @PathVariable Long providerId,
            @Valid @RequestBody CrearPedidoProveedorRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoProveedorService.crearBorrador(providerId, request));
    }

    @PatchMapping("/pedidos/{orderId}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PedidoProveedorResponse> enviar(@PathVariable Long orderId) {
        return ResponseEntity.ok(pedidoProveedorService.enviar(orderId));
    }

    @PatchMapping("/pedidos/{orderId}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PedidoProveedorResponse> recibir(@PathVariable Long orderId) {
        return ResponseEntity.ok(pedidoProveedorService.recibir(orderId));
    }

    @PatchMapping("/pedidos/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PedidoProveedorResponse> cancelar(@PathVariable Long orderId) {
        return ResponseEntity.ok(pedidoProveedorService.cancelar(orderId));
    }
}
