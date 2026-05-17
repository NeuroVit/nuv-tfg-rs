package com.nuv.tfgrs.features.reportes.service.ia;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class ReporteAnalisisIaService {

    private final ChatModel chatModel;
    private final ReporteAnalisisIaPromptBuilder promptBuilder;
    private static final Map<String, String> ES_REPLACEMENTS = createEsReplacements();

    public ReporteAnalisisIaResultado analizar(
            String tipoReporte,
            String datosReporteJson,
            Map<String, Object> contextoOrganizacional,
            String modelo,
            Duration timeout,
            String promptVersion
    ) throws Exception {
        String prompt = promptBuilder.build(tipoReporte, datosReporteJson, contextoOrganizacional, promptVersion);
        ChatClient client = ChatClient.create(chatModel);

        CompletableFuture<String> call = CompletableFuture.supplyAsync(() ->
                client.prompt()
                        .user(prompt)
                        .call()
                        .content()
        );

        try {
            String content = call.get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (content == null || content.isBlank()) {
                return new ReporteAnalisisIaResultado(
                        null,
                        ReporteAnalisisIaEstado.NO_DISPONIBLE,
                        modelo,
                        null,
                        "Ollama no devolvió contenido"
                );
            }

            String normalized = normalizeToSpanish(content.trim());
            return new ReporteAnalisisIaResultado(
                    normalized,
                    ReporteAnalisisIaEstado.GENERADO,
                    modelo,
                    LocalDateTime.now(),
                    null
            );
        } catch (TimeoutException e) {
            call.cancel(true);
            throw e;
        }
    }

    private String normalizeToSpanish(String text) {
        String normalized = text;
        for (Map.Entry<String, String> entry : ES_REPLACEMENTS.entrySet()) {
            normalized = normalized.replaceAll("(?i)\\b" + entry.getKey() + "\\b", entry.getValue());
        }
        return normalized;
    }

    private static Map<String, String> createEsReplacements() {
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("inventory", "inventario");
        replacements.put("supply\\s+chain", "cadena de suministro");
        replacements.put("insights", "hallazgos");
        replacements.put("insight", "hallazgo");
        replacements.put("growth", "crecimiento");
        replacements.put("trend", "tendencia");
        replacements.put("trends", "tendencias");
        replacements.put("performance", "rendimiento");
        replacements.put("business", "negocio");
        replacements.put("customers", "clientes");
        replacements.put("customer", "cliente");
        replacements.put("revenue", "ingresos");
        replacements.put("sales", "ventas");
        replacements.put("pipeline", "embudo");
        replacements.put("forecast", "proyección");
        replacements.put("risk", "riesgo");
        replacements.put("risks", "riesgos");
        replacements.put("stock\\s+low", "stock bajo");
        return replacements;
    }
}
