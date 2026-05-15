package com.nuv.tfgrs.features.bundles.service;

import com.nuv.tfgrs.features.bundles.dto.request.BundleUpsertRequest;
import com.nuv.tfgrs.features.bundles.dto.response.BundleItemResponse;
import com.nuv.tfgrs.features.bundles.dto.response.BundleSummaryResponse;

import java.util.List;

public interface BundleService {
    List<BundleSummaryResponse> listarBundles();
    List<BundleItemResponse> listarItemsDeBundle(Long bundleId);
    BundleSummaryResponse crear(BundleUpsertRequest request);
    BundleSummaryResponse actualizar(Long bundleId, BundleUpsertRequest request);
    void eliminar(Long bundleId);
}
