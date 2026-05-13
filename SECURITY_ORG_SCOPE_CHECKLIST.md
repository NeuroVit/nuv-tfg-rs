# Organization Scoping Checklist

Audit date: 2026-05-12

| Service | Method | Scoped by organizationId | Notes |
|---|---|---|---|
| `AlertaStockServiceImpl` | `listar` | Yes | Uses `requireMembership().organizationId()` + repo `findByOrganizationId...`. |
| `AlertaStockServiceImpl` | `resolver` | Yes | Uses `findByIdAndOrganizationId`. |
| `AnaliticaServiceImpl` | `inventarioResumen` | Yes | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `velocidadVentas` | Yes | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `tendenciasPrecios` | Yes | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `candidatosBundle` | Yes | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `velocidadResumen` | Yes | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `topProductosVelocidad` | Yes | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `distribucionStock` | Yes | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `stockPorCategoria` | Yes | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `finanzasResumen` | Yes | SQL filters `organization_id` for ventas/compras/costos. |
| `AnaliticaServiceImpl` | `rentabilidadProductos` | Yes | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `metadata` | Yes | SQL filters `organization_id`. |
| `AuthServiceImpl` | `registrar` | N/A | Global identity registration (no org yet). |
| `AuthServiceImpl` | `login` | N/A | Global authentication. |
| `AuthServiceImpl` | `loginConGoogle` | N/A | Global authentication/linking. |
| `AuthServiceImpl` | `refresh` | N/A | Token flow by user identity. |
| `AuthServiceImpl` | `cambiarRolUsuarioActual` | N/A | Self-account role cache switch (not org data read). |
| `AuthServiceImpl` | `perfilUsuarioActual` | N/A | Current user profile only. |
| `AuthServiceImpl` | `actualizarPerfilUsuarioActual` | N/A | Current user profile only. |
| `AuthServiceImpl` | `cambiarPasswordUsuarioActual` | N/A | Current user account only. |
| `AuthServiceImpl` | `revocarTokensUsuarioActual` | N/A | Current user account only. |
| `BundleServiceImpl` | `listarBundles` | Yes | SQL filters `b.organization_id`. |
| `BundleServiceImpl` | `listarItemsDeBundle` | Yes | SQL filters `bi.organization_id`. |
| `BundleServiceImpl` | `crear` | Yes | Inserts with `organization_id`; validates product ids in org. |
| `BundleServiceImpl` | `actualizar` | Yes | Updates/deletes constrained by `organization_id`. |
| `BundleServiceImpl` | `eliminar` | Yes | Deletes constrained by `organization_id`. |
| `CategoriaServiceImpl` | `listarTodas` | Yes | Repo by `organizationId`. |
| `CategoriaServiceImpl` | `buscarPorId` | Yes | Repo `findByIdAndOrganizationId`. |
| `CategoriaServiceImpl` | `crear` | Yes | Saves entity with `organizationId`. |
| `CategoriaServiceImpl` | `actualizar` | Yes | Repo `findByIdAndOrganizationId`. |
| `CategoriaServiceImpl` | `eliminar` | Yes | Repo `findByIdAndOrganizationId`. |
| `ClienteServiceImpl` | `listar` | Yes | Repo by `organizationId`. |
| `ClienteServiceImpl` | `crear` | Yes | Saves entity with `organizationId`. |
| `ClienteServiceImpl` | `actualizar` | Yes | Repo `findByIdAndOrganizationId`. |
| `ClienteServiceImpl` | `archivar` | Yes | Repo `findByIdAndOrganizationId`. |
| `ClienteServiceImpl` | `restaurar` | Yes | Repo `findByIdAndOrganizationId`. |
| `MovimientoStockQueryServiceImpl` | `listar` | Yes | SQL filter `sm.organization_id = :organizationId`. |
| `OrganizationServiceImpl` | `createOrganizationForCurrentUser` | N/A | Creates new org for current user. |
| `OrganizationServiceImpl` | `joinOrganizationForCurrentUser` | N/A | Explicit org join flow. |
| `OrganizationServiceImpl` | `searchOrganizations` | N/A | Global org discovery by design. |
| `OrganizationServiceImpl` | `updateCurrentOrganization` | Yes | Mutates only current membership org. |
| `OrganizationServiceImpl` | `currentSession` | Yes | Resolved from current membership context. |
| `PedidoProveedorServiceImpl` | `listarPorProveedor` | Yes | SQL filter `organization_id`. |
| `PedidoProveedorServiceImpl` | `crearBorrador` | Yes | Inserts + item checks constrained by `organization_id`. |
| `PedidoProveedorServiceImpl` | `enviar` | Yes | Update constrained by `organization_id`. |
| `PedidoProveedorServiceImpl` | `recibir` | Yes | Stock/movements constrained by `organization_id`. |
| `PedidoProveedorServiceImpl` | `cancelar` | Yes | Update constrained by `organization_id`. |
| `ProductoServiceImpl` | `listarTodos` | Yes | Repo by `organizationId`. |
| `ProductoServiceImpl` | `listarBajoStock` | Yes | SQL filter `organization_id`. |
| `ProductoServiceImpl` | `buscarPorId` | Yes | Repo `findByIdAndOrganizationId`. |
| `ProductoServiceImpl` | `crear` | Yes | Saves entity with `organizationId`; validates related entities in org. |
| `ProductoServiceImpl` | `actualizar` | Yes | Repo `findByIdAndOrganizationId`. |
| `ProductoServiceImpl` | `actualizarStock` | Yes | Product lookup + movement save with `organizationId`. |
| `ProductoServiceImpl` | `eliminar` | Yes | Product lookup constrained by org. |
| `ProductoServiceImpl` | `listarBundlesVinculados` | Yes | SQL filter `organization_id`. |
| `ProductoServiceImpl` | `desvincularDeBundles` | Yes | Delete constrained by `organization_id`. |
| `ProveedorServiceImpl` | `listar` | Yes | Repo by `organizationId`. |
| `ProveedorServiceImpl` | `crear` | Yes | Saves entity with `organizationId`. |
| `ProveedorServiceImpl` | `actualizar` | Yes | Repo `findByIdAndOrganizationId`. |
| `ProveedorServiceImpl` | `archivar` | Yes | Repo `findByIdAndOrganizationId`. |
| `ProveedorServiceImpl` | `restaurar` | Yes | Repo `findByIdAndOrganizationId`. |
| `ProveedorServiceImpl` | `detalle` | Yes | Provider + products + PO aggregates filter by `organization_id`. |
| `StockAlertManager` | `syncForProduct` | Yes | Uses product `organizationId` for alert upsert/query. |
| `UsuarioAdminServiceImpl` | `listar` | Yes | Membership listing by `organizationId`. |
| `UsuarioAdminServiceImpl` | `invitar` | Yes | Requires owner/admin + creates membership in current org. |
| `UsuarioAdminServiceImpl` | `actualizarRol` | Yes | User resolved via membership and org check. |
| `UsuarioAdminServiceImpl` | `desactivar` | Yes | User resolved via membership and org check. |
| `UsuarioAdminServiceImpl` | `activar` | Yes | User resolved via membership and org check. |
| `UsuarioAdminServiceImpl` | `auditoria` | Yes | SQL joins filtered by `om.organization_id`. |
| `VentaServiceImpl` | `listar` | Yes | SQL filter `s.organization_id`. |
| `VentaServiceImpl` | `crear` | Yes | Client/product/bundle validations and writes constrained by `organization_id`. |

## Findings

- No cross-organization data leak found in audited public service methods.
- `AnaliticaController` `@PreAuthorize("isAuthenticated()")` is acceptable with current matrix because service-layer queries are org-scoped and membershipless users are blocked by `requireMembership()`.
