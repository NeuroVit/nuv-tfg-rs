package com.nuv.tfgrs.features.ventas.service.impl;

import com.nuv.tfgrs.features.ventas.dto.response.VentaItemResponse;
import com.nuv.tfgrs.features.ventas.dto.response.VentaResponse;
import com.nuv.tfgrs.features.clientes.entity.Cliente;
import com.nuv.tfgrs.features.ventas.service.VentaEmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentaEmailServiceImpl implements VentaEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-ES");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(LOCALE_ES);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void enviarComprobanteVenta(VentaResponse venta, Cliente cliente) {
        if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
            log.info("Cliente {} sin email. Se omite envío de comprobante de venta #{}", cliente.getId(), venta.id());
            return;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("spring.mail.username no configurado. Se omite envío de comprobante de venta #{}", venta.id());
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender no disponible. Se omite envío de comprobante de venta #{}", venta.id());
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail, "NeuralStock");
            helper.setTo(cliente.getEmail().trim());
            helper.setSubject("NeuralStock | Comprobante de compra #" + venta.id());
            helper.setText(buildTextBody(venta, cliente), buildHtmlBody(venta, cliente));
            mailSender.send(mimeMessage);
            log.info("Comprobante de venta #{} enviado a cliente {} ({})", venta.id(), cliente.getId(), cliente.getEmail());
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo enviar el comprobante de venta", ex);
        }
    }

    private String buildTextBody(VentaResponse venta, Cliente cliente) {
        StringBuilder body = new StringBuilder();
        body.append("NeuralStock\n");
        body.append("Comprobante de compra #").append(venta.id()).append("\n\n");
        body.append("Cliente: ").append(cliente.getName()).append("\n");
        body.append("Fecha: ").append(venta.soldAt().format(DATE_TIME_FORMAT)).append("\n\n");
        body.append("Detalle:\n");

        for (VentaItemResponse item : venta.items()) {
            body.append("- ")
                    .append(item.productoNombre())
                    .append(" (").append(item.sku()).append(")")
                    .append(" | Cantidad: ").append(item.quantity())
                    .append(" | P. unitario: ").append(formatCurrency(item.unitPriceSnapshot()))
                    .append(" | Subtotal: ").append(formatCurrency(item.subtotal()))
                    .append("\n");
        }

        BigDecimal subtotal = venta.subtotalAmount() != null ? venta.subtotalAmount() : venta.totalAmount();
        BigDecimal descuento = venta.discountTotalAmount() != null ? venta.discountTotalAmount() : BigDecimal.ZERO;
        BigDecimal total = venta.totalAmount() != null ? venta.totalAmount() : BigDecimal.ZERO;

        body.append("\nSubtotal: ").append(formatCurrency(subtotal));
        body.append("\nDescuento: ").append(formatCurrency(descuento));
        body.append("\nTotal: ").append(formatCurrency(total));
        body.append("\n\nGracias por tu compra.\nEquipo NeuralStock");
        return body.toString();
    }

    private String buildHtmlBody(VentaResponse venta, Cliente cliente) {
        StringBuilder rows = new StringBuilder();
        for (VentaItemResponse item : venta.items()) {
            rows.append("<tr>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;'>").append(escapeHtml(item.productoNombre())).append("</td>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;'>").append(escapeHtml(item.sku())).append("</td>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:center;'>").append(item.quantity()).append("</td>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;'>").append(formatCurrency(item.unitPriceSnapshot())).append("</td>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;font-weight:600;'>").append(formatCurrency(item.subtotal())).append("</td>")
                    .append("</tr>");
        }

        BigDecimal subtotal = venta.subtotalAmount() != null ? venta.subtotalAmount() : venta.totalAmount();
        BigDecimal descuento = venta.discountTotalAmount() != null ? venta.discountTotalAmount() : BigDecimal.ZERO;
        BigDecimal total = venta.totalAmount() != null ? venta.totalAmount() : BigDecimal.ZERO;

        return """
                <div style='background:#f3f4f6;padding:24px;font-family:Arial,Helvetica,sans-serif;color:#111827;'>
                  <div style='max-width:760px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;'>
                    <div style='background:linear-gradient(135deg,#0f172a 0%%,#1f2937 100%%);padding:20px 24px;'>
                      <h1 style='margin:0;color:#ffffff;font-size:22px;'>NeuralStock</h1>
                      <p style='margin:6px 0 0 0;color:#cbd5e1;font-size:13px;'>Comprobante de compra</p>
                    </div>
                    <div style='padding:24px;'>
                      <h2 style='margin:0 0 10px 0;font-size:20px;'>Compra #%d</h2>
                      <p style='margin:0 0 2px 0;font-size:14px;color:#374151;'><strong>Cliente:</strong> %s</p>
                      <p style='margin:0 0 16px 0;font-size:14px;color:#374151;'><strong>Fecha:</strong> %s</p>

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
                        <tbody>%s</tbody>
                      </table>

                      <div style='margin-top:16px;display:flex;justify-content:flex-end;'>
                        <div style='min-width:280px;'>
                          <div style='display:flex;justify-content:space-between;font-size:14px;color:#4b5563;padding:4px 0;'>
                            <span>Subtotal</span><strong>%s</strong>
                          </div>
                          <div style='display:flex;justify-content:space-between;font-size:14px;color:#4b5563;padding:4px 0;'>
                            <span>Descuento</span><strong>%s</strong>
                          </div>
                          <div style='display:flex;justify-content:space-between;font-size:20px;color:#0f766e;padding:8px 0 0 0;border-top:1px solid #e5e7eb;margin-top:6px;'>
                            <span>Total</span><strong>%s</strong>
                          </div>
                        </div>
                      </div>

                      <p style='margin:24px 0 0 0;font-size:13px;color:#6b7280;'>Gracias por confiar en NeuralStock.</p>
                    </div>
                  </div>
                </div>
                """.formatted(
                venta.id(),
                escapeHtml(cliente.getName()),
                venta.soldAt().format(DATE_TIME_FORMAT),
                rows,
                formatCurrency(subtotal),
                formatCurrency(descuento),
                formatCurrency(total)
        );
    }

    private String formatCurrency(BigDecimal value) {
        return CURRENCY_FORMAT.format(value != null ? value : BigDecimal.ZERO);
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
