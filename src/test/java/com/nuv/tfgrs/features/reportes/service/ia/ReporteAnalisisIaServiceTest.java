package com.nuv.tfgrs.features.reportes.service.ia;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReporteAnalisisIaServiceTest {

    @Test
    void normalizacionDebeReemplazarTerminosComunesEnIngles() {
        ReporteAnalisisIaService service = new ReporteAnalisisIaService(null, new ReporteAnalisisIaPromptBuilder());
        String raw = "The inventory has low stock and supply chain issues with growth trend.";

        String normalized = (String) ReflectionTestUtils.invokeMethod(service, "normalizeToSpanish", raw);

        assertTrue(normalized.contains("inventario"));
        assertTrue(normalized.contains("cadena de suministro"));
        assertTrue(normalized.contains("crecimiento"));
        assertTrue(normalized.contains("tendencia"));
        assertFalse(normalized.toLowerCase().contains("inventory"));
    }
}
