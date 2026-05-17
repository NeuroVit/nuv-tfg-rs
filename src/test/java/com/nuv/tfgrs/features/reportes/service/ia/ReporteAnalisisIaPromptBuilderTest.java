package com.nuv.tfgrs.features.reportes.service.ia;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReporteAnalisisIaPromptBuilderTest {

    private final ReporteAnalisisIaPromptBuilder builder = new ReporteAnalisisIaPromptBuilder();

    @Test
    void debeIncluirTipoDatosContextoEInstruccionesAntiInvencion() {
        String datos = "{\"totalVentas\":10,\"ingresosMes\":1234.5}";
        Map<String, Object> contexto = Map.of("ventas", Map.of("totalVentas", 120L));

        String prompt = builder.build("VENTAS", datos, contexto, "v1");

        assertTrue(prompt.contains("Tipo de reporte:\nVENTAS"));
        assertTrue(prompt.contains("Datos exactos del reporte (JSON):\n" + datos));
        assertTrue(prompt.contains("Contexto histórico agregado de la organización (JSON):"));
        assertTrue(prompt.contains("No inventes cifras, hechos ni conclusiones"));
        assertTrue(prompt.contains("4 a 6 frases"));
    }
}
