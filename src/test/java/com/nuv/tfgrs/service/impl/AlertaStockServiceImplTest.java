package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.entity.AlertaStock;
import com.nuv.tfgrs.entity.Organization;
import com.nuv.tfgrs.entity.Producto;
import com.nuv.tfgrs.repository.AlertaStockRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaStockServiceImplTest {

    @Mock
    private AlertaStockRepository alertaStockRepository;

    @Mock
    private CurrentOrganizationResolver currentOrganizationResolver;

    private AlertaStockServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AlertaStockServiceImpl(alertaStockRepository, currentOrganizationResolver);

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
        assertTrue(ex.getReason().contains("No se puede resolver la alerta"));
    }

    @Test
    void resolverLowStockDebePermitirCuandoYaSeRepuso() {
        AlertaStock alert = buildStockAlert(2L, AlertaStock.AlertType.LOW_STOCK, 8, 5);
        when(alertaStockRepository.findByIdAndOrganizationId(2L, 10L)).thenReturn(Optional.of(alert));
        when(alertaStockRepository.save(any(AlertaStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.resolver(2L);

        assertTrue(alert.getResolved());
        assertNotNull(alert.getResolvedAt());
        assertEquals(2L, response.id());
        verify(alertaStockRepository).save(eq(alert));
    }

    @Test
    void resolverAlertaNoStockSeMantienePermitido() {
        AlertaStock alert = buildStockAlert(3L, AlertaStock.AlertType.OVERSTOCK, 1, 5);
        when(alertaStockRepository.findByIdAndOrganizationId(3L, 10L)).thenReturn(Optional.of(alert));
        when(alertaStockRepository.save(any(AlertaStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.resolver(3L);

        assertTrue(alert.getResolved());
        assertNotNull(alert.getResolvedAt());
        assertEquals(3L, response.id());
        verify(alertaStockRepository).save(eq(alert));
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
