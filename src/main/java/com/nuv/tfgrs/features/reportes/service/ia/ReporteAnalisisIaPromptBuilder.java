package com.nuv.tfgrs.features.reportes.service.ia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReporteAnalisisIaPromptBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String build(String tipoReporte, String datosReporteJson, Map<String, Object> contextoOrganizacional, String promptVersion) {
        String rol = resolveRol(tipoReporte);
        String contextoJson = safeJson(contextoOrganizacional);

        return """
                Prompt version: %s

                Rol del asistente:
                %s

                Instrucciones obligatorias:
                - Usa exclusivamente los datos proporcionados en esta entrada.
                - No inventes cifras, hechos ni conclusiones que no estén respaldadas por los datos.
                - Si falta información para una afirmación, dilo explícitamente.
                - Mantén un tono profesional, claro y conciso.
                - Escribe todo en español neutro.
                - No uses palabras en inglés ni anglicismos (por ejemplo: inventory, supply chain, insights, growth, trend, performance).
                - Identifica riesgos relevantes, tendencias positivas y termina con 1 a 2 recomendaciones accionables.

                Tipo de reporte:
                %s

                Datos exactos del reporte (JSON):
                %s

                Contexto histórico agregado de la organización (JSON):
                %s

                Salida esperada:
                - Responde en español.
                - Entrega de 4 a 6 frases.
                - No uses viñetas.
                """.formatted(
                promptVersion,
                rol,
                tipoReporte,
                datosReporteJson == null ? "{}" : datosReporteJson,
                contextoJson
        );
    }

    private String resolveRol(String tipoReporte) {
        return switch (tipoReporte) {
            case "INVENTARIO" -> "Analista senior de inventario y rotación de stock.";
            case "ALERTAS" -> "Analista de riesgos operativos y alertas de stock.";
            case "CLIENTES" -> "Analista de clientes y comportamiento comercial.";
            case "MOVIMIENTOS" -> "Analista de operaciones logísticas y trazabilidad de stock.";
            case "VENTAS" -> "Analista financiero comercial y rendimiento de ventas.";
            default -> "Analista de negocio para reportes operativos.";
        };
    }

    private String safeJson(Map<String, Object> payload) {
        try {
            return MAPPER.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
