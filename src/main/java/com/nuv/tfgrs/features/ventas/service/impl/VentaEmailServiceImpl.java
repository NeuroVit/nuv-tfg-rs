package com.nuv.tfgrs.features.ventas.service.impl;

import com.nuv.tfgrs.features.ventas.dto.response.VentaItemResponse;
import com.nuv.tfgrs.features.ventas.dto.response.VentaResponse;
import com.nuv.tfgrs.features.clientes.entity.Cliente;
import com.nuv.tfgrs.infrastructure.email.EmailDispatchService;
import com.nuv.tfgrs.features.ventas.service.VentaEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentaEmailServiceImpl implements VentaEmailService {

    private final EmailDispatchService emailDispatchService;

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-ES");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(LOCALE_ES);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void enviarComprobanteVenta(VentaResponse venta, Cliente cliente) {
        if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
            log.info("Cliente {} sin email. Se omite envio de comprobante de venta #{}", cliente.getId(), venta.id());
            return;
        }

        try {
            emailDispatchService.sendEmail(
                    "NeuralStock",
                    cliente.getEmail().trim(),
                    "NeuralStock | Comprobante de compra #" + venta.id(),
                    buildTextBody(venta, cliente),
                    buildHtmlBody(venta, cliente)
            );
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
                <div style='background:#eef2f7;padding:24px;font-family:Arial,Helvetica,sans-serif;color:#111827;'>
                  <div style='max-width:760px;margin:0 auto;background:#ffffff;border:1px solid #dbe2ea;border-radius:14px;overflow:hidden;'>
                    <div style='background:linear-gradient(135deg,#0f172a 0%%,#1e293b 42%%,#0ea5a4 100%%);padding:20px 24px;'>
                      <table role='presentation' width='100%%' style='border-collapse:collapse;'>
                        <tr>
                          <td style='color:#ffffff;'>
                            <h1 style='margin:0;font-size:22px;'>NeuralStock</h1>
                            <p style='margin:6px 0 0 0;color:#c7d2fe;font-size:13px;'>Comprobante de compra</p>
                          </td>
                          <td style='text-align:right;color:#ffffff;vertical-align:top;'>
                            <p style='margin:0;font-size:11px;opacity:0.9;'>DOCUMENTO</p>
                            <p style='margin:4px 0 0 0;font-size:18px;font-weight:700;letter-spacing:0.3px;'>CLIENTE</p>
                          </td>
                        </tr>
                      </table>
                    </div>
                    <div style='padding:24px;'>
                      <table role='presentation' width='100%%' style='border-collapse:collapse;margin-bottom:14px;'>
                        <tr>
                          <td style='width:58%%;vertical-align:top;padding-right:12px;'>
                            <h2 style='margin:0 0 12px 0;font-size:20px;color:#0f172a;'>Compra #%d</h2>
                            <p style='margin:0 0 4px 0;font-size:14px;color:#334155;'><strong>Cliente:</strong> %s</p>
                            <p style='margin:0;font-size:14px;color:#334155;'><strong>Fecha:</strong> %s</p>
                          </td>
                          <td style='width:42%%;vertical-align:top;'>
                            <div style='border:1px solid #dbe2ea;border-radius:10px;padding:12px 14px;background:#f8fafc;'>
                              <p style='margin:0 0 6px 0;font-size:11px;color:#64748b;text-transform:uppercase;letter-spacing:0.05em;'>Estado</p>
                              <p style='margin:0;font-size:14px;font-weight:700;color:#0f766e;'>Procesada correctamente</p>
                            </div>
                          </td>
                        </tr>
                      </table>

                      <table style='width:100%%;border-collapse:collapse;border:1px solid #dbe2ea;border-radius:10px;overflow:hidden;'>
                        <thead>
                          <tr style='background:#0f172a;'>
                            <th style='padding:11px 10px;text-align:left;font-size:11px;color:#e2e8f0;text-transform:uppercase;letter-spacing:0.05em;'>Producto</th>
                            <th style='padding:11px 10px;text-align:left;font-size:11px;color:#e2e8f0;text-transform:uppercase;letter-spacing:0.05em;'>SKU</th>
                            <th style='padding:11px 10px;text-align:center;font-size:11px;color:#e2e8f0;text-transform:uppercase;letter-spacing:0.05em;'>Cantidad</th>
                            <th style='padding:11px 10px;text-align:right;font-size:11px;color:#e2e8f0;text-transform:uppercase;letter-spacing:0.05em;'>P. unitario</th>
                            <th style='padding:11px 10px;text-align:right;font-size:11px;color:#e2e8f0;text-transform:uppercase;letter-spacing:0.05em;'>Subtotal</th>
                          </tr>
                        </thead>
                        <tbody>%s</tbody>
                      </table>

                      <div style='margin-top:16px;display:flex;justify-content:flex-end;'>
                        <div style='min-width:280px;'>
                            <div style='display:flex;justify-content:space-between;gap:12px;font-size:14px;color:#4b5563;padding:4px 0;'>
                              <span>Subtotal =</span><strong style='white-space:nowrap;'>%s</strong>
                            </div>
                            <div style='display:flex;justify-content:space-between;gap:12px;font-size:14px;color:#4b5563;padding:4px 0;'>
                              <span>Descuento =</span><strong style='white-space:nowrap;'>%s</strong>
                            </div>
                            <div style='display:flex;justify-content:space-between;gap:12px;font-size:20px;color:#0f766e;padding:8px 0 0 0;border-top:1px solid #e5e7eb;margin-top:6px;'>
                              <span>Total =</span><strong style='white-space:nowrap;'>%s</strong>
                            </div>
                        </div>
                      </div>

                      <div style='margin-top:20px;background:linear-gradient(135deg,#dcfce7 0%%,#ccfbf1 100%%);border:1px solid #99f6e4;border-radius:10px;padding:14px 16px;'>
                        <p style='margin:0;font-size:15px;color:#065f46;font-weight:700;'>Gracias por su compra.</p>
                        <p style='margin:6px 0 0 0;font-size:13px;color:#0f766e;'>Su comprobante queda registrado en NeuralStock. Esperamos volver a atenderle pronto.</p>
                      </div>
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
