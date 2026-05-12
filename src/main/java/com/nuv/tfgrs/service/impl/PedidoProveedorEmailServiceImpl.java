package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.response.PedidoProveedorItemResponse;
import com.nuv.tfgrs.dto.response.PedidoProveedorResponse;
import com.nuv.tfgrs.entity.Proveedor;
import com.nuv.tfgrs.service.PedidoProveedorEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoProveedorEmailServiceImpl implements PedidoProveedorEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-ES");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(LOCALE_ES);

    @Override
    public void enviarPedidoAProveedor(PedidoProveedorResponse pedido, Proveedor proveedor) {
        if (proveedor.getEmail() == null || proveedor.getEmail().isBlank()) {
            log.info("Proveedor {} sin email. Se omite envio de pedido #{}", proveedor.getId(), pedido.id());
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, "NeuralStock");
            helper.setTo(proveedor.getEmail().trim());
            helper.setSubject("NeuralStock | Pedido #" + pedido.id() + " - " + pedido.providerNombre());
            helper.setText(buildTextBody(pedido), buildHtmlBody(pedido));

            mailSender.send(mimeMessage);
            log.info("Correo de pedido #{} enviado a proveedor {} ({})", pedido.id(), proveedor.getId(), proveedor.getEmail());
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo enviar el correo del pedido", ex);
        }
    }

    private String buildTextBody(PedidoProveedorResponse pedido) {
        StringBuilder body = new StringBuilder();
        body.append("NeuralStock\n");
        body.append("Pedido #").append(pedido.id()).append("\n\n");
        body.append("Proveedor: ").append(pedido.providerNombre()).append("\n");
        body.append("Fecha de llegada deseada: ").append(pedido.fechaLlegadaDeseada().format(DATE_FORMAT)).append("\n");
        body.append("Fecha de pedido recomendada: ").append(pedido.fechaPedidoRecomendada().format(DATE_FORMAT)).append("\n\n");
        body.append("Detalle de productos:\n");

        BigDecimal totalEstimado = BigDecimal.ZERO;
        for (PedidoProveedorItemResponse item : pedido.items()) {
            BigDecimal subtotal = item.precioUnitarioSnapshot().multiply(BigDecimal.valueOf(item.cantidad()));
            totalEstimado = totalEstimado.add(subtotal);
            body.append("- ")
                    .append(item.productoNombre())
                    .append(" (").append(item.sku()).append(")")
                    .append(" | Cantidad: ").append(item.cantidad())
                    .append(" | Precio unitario: ").append(formatCurrency(item.precioUnitarioSnapshot()))
                    .append(" | Subtotal: ").append(formatCurrency(subtotal))
                    .append("\n");
        }

        body.append("\nImporte estimado total: ").append(formatCurrency(totalEstimado)).append("\n");
        if (pedido.notas() != null && !pedido.notas().isBlank()) {
            body.append("\nNotas: ").append(pedido.notas().trim()).append("\n");
        }

        body.append("\nGracias,\nEquipo NeuralStock");
        return body.toString();
    }

    private String buildHtmlBody(PedidoProveedorResponse pedido) {
        BigDecimal totalEstimado = BigDecimal.ZERO;
        StringBuilder rows = new StringBuilder();

        for (PedidoProveedorItemResponse item : pedido.items()) {
            BigDecimal subtotal = item.precioUnitarioSnapshot().multiply(BigDecimal.valueOf(item.cantidad()));
            totalEstimado = totalEstimado.add(subtotal);
            rows.append("<tr>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;'>").append(escapeHtml(item.productoNombre())).append("</td>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;'>").append(escapeHtml(item.sku())).append("</td>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:center;'>").append(item.cantidad()).append("</td>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;'>").append(formatCurrency(item.precioUnitarioSnapshot())).append("</td>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;font-weight:600;'>").append(formatCurrency(subtotal)).append("</td>")
                    .append("</tr>");
        }

        String notesBlock = "";
        if (pedido.notas() != null && !pedido.notas().isBlank()) {
            notesBlock = "<div style='margin-top:16px;padding:12px;background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;'>"
                    + "<p style='margin:0 0 6px 0;font-size:12px;color:#6b7280;text-transform:uppercase;letter-spacing:0.04em;'>Notas</p>"
                    + "<p style='margin:0;font-size:14px;color:#111827;'>" + escapeHtml(pedido.notas().trim()) + "</p>"
                    + "</div>";
        }

        return """
                <div style='background:#f3f4f6;padding:24px;font-family:Arial,Helvetica,sans-serif;color:#111827;'>
                  <div style='max-width:760px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;'>
                    <div style='background:linear-gradient(135deg,#0f172a 0%%,#1f2937 100%%);padding:20px 24px;'>
                      <h1 style='margin:0;color:#ffffff;font-size:22px;'>NeuralStock</h1>
                      <p style='margin:6px 0 0 0;color:#cbd5e1;font-size:13px;'>Gestión inteligente de inventario</p>
                    </div>
                    <div style='padding:24px;'>
                      <h2 style='margin:0 0 14px 0;font-size:20px;color:#111827;'>Pedido #%d</h2>
                      <p style='margin:0 0 16px 0;font-size:14px;color:#4b5563;'>
                        Se ha generado un nuevo pedido para <strong>%s</strong>.
                      </p>

                      <div style='display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:18px;'>
                        <div style='padding:12px;border:1px solid #e5e7eb;border-radius:8px;'>
                          <p style='margin:0;font-size:12px;color:#6b7280;text-transform:uppercase;letter-spacing:0.04em;'>Llegada deseada</p>
                          <p style='margin:6px 0 0 0;font-size:15px;font-weight:600;'>%s</p>
                        </div>
                        <div style='padding:12px;border:1px solid #e5e7eb;border-radius:8px;'>
                          <p style='margin:0;font-size:12px;color:#6b7280;text-transform:uppercase;letter-spacing:0.04em;'>Pedido recomendado</p>
                          <p style='margin:6px 0 0 0;font-size:15px;font-weight:600;'>%s</p>
                        </div>
                      </div>

                      <table style='width:100%%;border-collapse:collapse;border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;'>
                        <thead>
                          <tr style='background:#f9fafb;'>
                            <th style='padding:10px;text-align:left;font-size:12px;color:#374151;text-transform:uppercase;'>Producto</th>
                            <th style='padding:10px;text-align:left;font-size:12px;color:#374151;text-transform:uppercase;'>SKU</th>
                            <th style='padding:10px;text-align:center;font-size:12px;color:#374151;text-transform:uppercase;'>Cantidad</th>
                            <th style='padding:10px;text-align:right;font-size:12px;color:#374151;text-transform:uppercase;'>P. unitario</th>
                            <th style='padding:10px;text-align:right;font-size:12px;color:#374151;text-transform:uppercase;'>Subtotal</th>
                          </tr>
                        </thead>
                        <tbody>
                          %s
                        </tbody>
                      </table>

                      <div style='margin-top:16px;text-align:right;'>
                        <p style='margin:0;font-size:13px;color:#6b7280;'>Importe estimado total</p>
                        <p style='margin:4px 0 0 0;font-size:22px;font-weight:700;color:#0f766e;'>%s</p>
                      </div>

                      %s

                      <p style='margin:24px 0 0 0;font-size:13px;color:#6b7280;'>
                        Gracias por colaborar con NeuralStock.
                      </p>
                    </div>
                  </div>
                </div>
                """.formatted(
                pedido.id(),
                escapeHtml(pedido.providerNombre()),
                pedido.fechaLlegadaDeseada().format(DATE_FORMAT),
                pedido.fechaPedidoRecomendada().format(DATE_FORMAT),
                rows,
                formatCurrency(totalEstimado),
                notesBlock
        );
    }

    private String formatCurrency(BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }

    private String escapeHtml(String value) {
        return value == null
                ? ""
                : value.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#39;");
    }
}
