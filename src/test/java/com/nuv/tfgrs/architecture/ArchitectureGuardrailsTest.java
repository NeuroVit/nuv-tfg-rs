package com.nuv.tfgrs.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureGuardrailsTest {

    private static final Path SOURCE_ROOT = Paths.get("src/main/java/com/nuv/tfgrs");

    private static final int DEFAULT_MAX_FILE_LINES = 600;
    private static final int DEFAULT_MAX_METHOD_COMPLEXITY = 12;

    private static final Map<String, Integer> FILE_LINE_LIMIT_OVERRIDES = Map.of(
            "VentaServiceImpl.java", 1200,
            "AnaliticaServiceImpl.java", 1700,
            "ProductoServiceImpl.java", 700,
            "PedidoProveedorServiceImpl.java", 700,
            "AuthController.java", 700
    );

    private static final Map<String, Integer> METHOD_COMPLEXITY_LIMIT_OVERRIDES = Map.of(
            "VentaServiceImpl.java", 25,
            "AnaliticaServiceImpl.java", 30,
            "ProductoServiceImpl.java", 20,
            "PedidoProveedorServiceImpl.java", 20,
            "ReporteServiceImpl.java", 16
    );

    private static final Pattern METHOD_START_PATTERN = Pattern.compile(
            "^\\s*(public|protected|private)\\s+(?!class\\b)(?!record\\b)(?!enum\\b)(?!interface\\b).+\\)\\s*\\{\\s*$"
    );

    private static final Pattern COMPLEXITY_TOKEN_PATTERN = Pattern.compile("\\b(if|for|while|catch|case)\\b|\\?");

    @Test
    void filesShouldRespectMaximumLineCount() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : listJavaFiles()) {
            String fileName = file.getFileName().toString();
            int maxLines = FILE_LINE_LIMIT_OVERRIDES.getOrDefault(fileName, DEFAULT_MAX_FILE_LINES);
            long lines = Files.lines(file).count();
            if (lines > maxLines) {
                violations.add(file + " has " + lines + " lines (max " + maxLines + ")");
            }
        }

        assertTrue(violations.isEmpty(), "File size guardrail violations:\n" + String.join("\n", violations));
    }

    @Test
    void methodsShouldRespectComplexityThreshold() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : listJavaFiles()) {
            int maxComplexity = METHOD_COMPLEXITY_LIMIT_OVERRIDES.getOrDefault(
                    file.getFileName().toString(),
                    DEFAULT_MAX_METHOD_COMPLEXITY
            );
            Map<String, Integer> complexityByMethod = extractMethodComplexity(file);
            complexityByMethod.forEach((methodName, complexity) -> {
                if (complexity > maxComplexity) {
                    violations.add(file + "::" + methodName + " complexity=" + complexity + " (max " + maxComplexity + ")");
                }
            });
        }

        assertTrue(violations.isEmpty(), "Method complexity guardrail violations:\n" + String.join("\n", violations));
    }

    @Test
    void sourceShouldRespectLayerBoundaries() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : listJavaFiles()) {
            String normalized = file.toString().replace('\\', '/');
            String content = Files.readString(file);

            if (normalized.contains("/controller/") && content.contains("import com.nuv.tfgrs.repository.")) {
                violations.add(file + " controller must not import repository directly");
            }
            if (normalized.contains("/repository/") && content.contains("import com.nuv.tfgrs.controller.")) {
                violations.add(file + " repository must not import controller");
            }
            if (normalized.contains("/dto/") && content.contains("import com.nuv.tfgrs.service.")) {
                violations.add(file + " dto must not import service");
            }
            if (normalized.contains("/features/") && content.contains("import com.nuv.tfgrs.controller.")) {
                violations.add(file + " feature internals must not import controllers");
            }
        }

        assertTrue(violations.isEmpty(), "Layer boundary violations:\n" + String.join("\n", violations));
    }

    private static List<Path> listJavaFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(SOURCE_ROOT)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static Map<String, Integer> extractMethodComplexity(Path file) throws IOException {
        Map<String, Integer> result = new HashMap<>();
        List<String> lines = Files.readAllLines(file);

        boolean insideMethod = false;
        String currentMethod = null;
        int bracesBalance = 0;
        int complexity = 1;

        for (String line : lines) {
            if (!insideMethod) {
                Matcher matcher = METHOD_START_PATTERN.matcher(line);
                if (!matcher.find()) {
                    continue;
                }

                currentMethod = extractMethodName(line);
                insideMethod = true;
                bracesBalance = countChar(line, '{') - countChar(line, '}');
                complexity = 1 + countComplexityTokens(line);
                continue;
            }

            bracesBalance += countChar(line, '{');
            bracesBalance -= countChar(line, '}');
            complexity += countComplexityTokens(line);

            if (bracesBalance <= 0) {
                result.put(currentMethod == null ? "anonymous" : currentMethod, complexity);
                insideMethod = false;
                currentMethod = null;
                complexity = 1;
            }
        }

        return result;
    }

    private static int countComplexityTokens(String line) {
        Matcher matcher = COMPLEXITY_TOKEN_PATTERN.matcher(line);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static int countChar(String value, char needle) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == needle) {
                count++;
            }
        }
        return count;
    }

    private static String extractMethodName(String signatureLine) {
        int parenIndex = signatureLine.indexOf('(');
        if (parenIndex <= 0) {
            return "unknown";
        }

        String head = signatureLine.substring(0, parenIndex).trim();
        String[] tokens = head.split("\\s+");
        return tokens.length == 0 ? "unknown" : tokens[tokens.length - 1];
    }
}
