package com.nuv.tfgrs.features.alertas.service.impl;

import com.nuv.tfgrs.features.alertas.entity.AlertaStock;
import com.nuv.tfgrs.features.organizaciones.entity.Organization;
import com.nuv.tfgrs.features.productos.entity.Producto;
import com.nuv.tfgrs.features.alertas.repository.AlertaStockRepository;
import com.nuv.tfgrs.features.productos.repository.ProductoRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.security.org.OrganizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaStockServiceImplTest {

    @Mock
    private AlertaStockRepository alertaStockRepository;

    @Mock
    private CurrentOrganizationResolver currentOrganizationResolver;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private StockAlertManager stockAlertManager;

    private AlertaStockServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AlertaStockServiceImpl(
                alertaStockRepository,
                productoRepository,
                currentOrganizationResolver,
                stockAlertManager
        );

        Organization organization = new Organization();
        organization.setId(10L);
        when(currentOrganizationResolver.requireMembership())
                .thenReturn(new OrganizationContext(null, organization, null));
    }

    @Test
    void resolverLowStockDebeFallarSiSigueDebajoDelMinimo() {
        AlertaStock alert = buildStockAlert(1L, AlertaStock.AlertType.LOW_STOCK, 2, 5);
        when(alertaStockRepository.findByIdAndOrganizationId(1L, 10L)).thenReturn(Optional.of(alert));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.resolver(1L));

        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("se resuelven automáticamente"));
    }

    @Test
    void resolverSiempreDebeFallarPorqueLasAlertasSonAutomaticas() {
        AlertaStock alert = buildStockAlert(2L, AlertaStock.AlertType.OVERSTOCK, 8, 5);
        when(alertaStockRepository.findByIdAndOrganizationId(2L, 10L)).thenReturn(Optional.of(alert));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.resolver(2L));
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("se resuelven automáticamente"));
    }

    private AlertaStock buildStockAlert(Long alertId, AlertaStock.AlertType type, int current, int minimum) {
        Producto producto = new Producto();
        producto.setId(99L);
        producto.setName("Producto test");
        producto.setSku("SKU-TEST");
        producto.setCurrentQuantity(current);
        producto.setMinimumStockLevel(minimum);

        AlertaStock alert = new AlertaStock();
        alert.setId(alertId);
        alert.setOrganizationId(10L);
        alert.setProduct(producto);
        alert.setAlertType(type);
        alert.setMessage("Alerta test");
        alert.setResolved(false);
        return alert;
    }
}
