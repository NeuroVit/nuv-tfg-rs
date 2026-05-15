package com.nuv.tfgrs.features.ventas.domain;

import com.nuv.tfgrs.features.ventas.dto.request.CrearVentaRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class VentaRequestValidator {

    public void validateNoRepeatedProductsInDirectItems(List<CrearVentaRequest.Item> items) {
        Set<Long> uniqueProductIds = new HashSet<>();
        for (CrearVentaRequest.Item item : items) {
            if (!uniqueProductIds.add(item.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede repetir un producto en la venta");
            }
        }
    }

    public void validateNoRepeatedBundles(List<CrearVentaRequest.BundleItem> bundles) {
        Set<Long> uniqueBundleIds = new HashSet<>();
        for (CrearVentaRequest.BundleItem bundle : bundles) {
            if (!uniqueBundleIds.add(bundle.bundleId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede repetir un bundle en la venta");
            }
        }
    }
}
