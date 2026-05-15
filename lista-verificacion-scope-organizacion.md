# Lista de verificación de scope por organización

Fecha de auditoría: 2026-05-12

| Servicio | Método | Scope por organizationId | Notas |
|---|---|---|---|
| `AlertaStockServiceImpl` | `listar` | Sí | Uses `requireMembership().organizationId()` + repo `findByOrganizationId...`. |
| `AlertaStockServiceImpl` | `resolver` | Sí | Uses `findByIdAndOrganizationId`. |
| `AnaliticaServiceImpl` | `inventarioResumen` | Sí | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `velocidadVentas` | Sí | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `tendenciasPrecios` | Sí | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `candidatosBundle` | Sí | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `velocidadResumen` | Sí | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `topProductosVelocidad` | Sí | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `distribucionStock` | Sí | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `stockPorCategoria` | Sí | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `finanzasResumen` | Sí | SQL filters `organization_id` for ventas/compras/costos. |
| `AnaliticaServiceImpl` | `rentabilidadProductos` | Sí | SQL filters `organization_id`. |
| `AnaliticaServiceImpl` | `metadata` | Sí | SQL filters `organization_id`. |
| `AuthServiceImpl` | `registrar` | N/A | Global identity registration (no org yet). |
| `AuthServiceImpl` | `login` | N/A | Global authentication. |
| `AuthServiceImpl` | `loginConGoogle` | N/A | Global authentication/linking. |
| `AuthServiceImpl` | `refresh` | N/A | Token flow by user identity. |
| `AuthServiceImpl` | `cambiarRolUsuarioActual` | N/A | Self-account role cache switch (not org data read). |
| `AuthServiceImpl` | `perfilUsuarioActual` | N/A | Solo perfil del usuario actual. |
| `AuthServiceImpl` | `actualizarPerfilUsuarioActual` | N/A | Solo perfil del usuario actual. |
| `AuthServiceImpl` | `cambiarPasswordUsuarioActual` | N/A | Solo cuenta del usuario actual. |
| `AuthServiceImpl` | `revocarTokensUsuarioActual` | N/A | Solo cuenta del usuario actual. |
| `BundleServiceImpl` | `listarBundles` | Sí | SQL filters `b.organization_id`. |
| `BundleServiceImpl` | `listarItemsDeBundle` | Sí | SQL filters `bi.organization_id`. |
| `BundleServiceImpl` | `crear` | Sí | Inserts with `organization_id`; validates product ids in org. |
| `BundleServiceImpl` | `actualizar` | Sí | Updates/deletes constrained by `organization_id`. |
| `BundleServiceImpl` | `eliminar` | Sí | Deletes constrained by `organization_id`. |
| `CategoriaServiceImpl` | `listarTodas` | Sí | Repo by `organizationId`. |
| `CategoriaServiceImpl` | `buscarPorId` | Sí | Repo `findByIdAndOrganizationId`. |
| `CategoriaServiceImpl` | `crear` | Sí | Saves entity with `organizationId`. |
| `CategoriaServiceImpl` | `actualizar` | Sí | Repo `findByIdAndOrganizationId`. |
| `CategoriaServiceImpl` | `eliminar` | Sí | Repo `findByIdAndOrganizationId`. |
| `ClienteServiceImpl` | `listar` | Sí | Repo by `organizationId`. |
| `ClienteServiceImpl` | `crear` | Sí | Saves entity with `organizationId`. |
| `ClienteServiceImpl` | `actualizar` | Sí | Repo `findByIdAndOrganizationId`. |
| `ClienteServiceImpl` | `archivar` | Sí | Repo `findByIdAndOrganizationId`. |
| `ClienteServiceImpl` | `restaurar` | Sí | Repo `findByIdAndOrganizationId`. |
| `MovimientoStockQueryServiceImpl` | `listar` | Sí | SQL filter `sm.organization_id = :organizationId`. |
| `OrganizationServiceImpl` | `createOrganizationForCurrentUser` | N/A | Creates new org for current user. |
| `OrganizationServiceImpl` | `joinOrganizationForCurrentUser` | N/A | Explicit org join flow. |
| `OrganizationServiceImpl` | `searchOrganizations` | N/A | Global org discovery by design. |
| `OrganizationServiceImpl` | `updateCurrentOrganization` | Sí | Mutates only current membership org. |
| `OrganizationServiceImpl` | `currentSession` | Sí | Resolved from current membership context. |
| `PedidoProveedorServiceImpl` | `listarPorProveedor` | Sí | SQL filter `organization_id`. |
| `PedidoProveedorServiceImpl` | `crearBorrador` | Sí | Inserts + item checks constrained by `organization_id`. |
| `PedidoProveedorServiceImpl` | `enviar` | Sí | Update constrained by `organization_id`. |
| `PedidoProveedorServiceImpl` | `recibir` | Sí | Stock/movements constrained by `organization_id`. |
| `PedidoProveedorServiceImpl` | `cancelar` | Sí | Update constrained by `organization_id`. |
| `ProductoServiceImpl` | `listarTodos` | Sí | Repo by `organizationId`. |
| `ProductoServiceImpl` | `listarBajoStock` | Sí | SQL filter `organization_id`. |
| `ProductoServiceImpl` | `buscarPorId` | Sí | Repo `findByIdAndOrganizationId`. |
| `ProductoServiceImpl` | `crear` | Sí | Saves entity with `organizationId`; validates related entities in org. |
| `ProductoServiceImpl` | `actualizar` | Sí | Repo `findByIdAndOrganizationId`. |
| `ProductoServiceImpl` | `actualizarStock` | Sí | Product lookup + movement save with `organizationId`. |
| `ProductoServiceImpl` | `eliminar` | Sí | Product lookup constrained by org. |
| `ProductoServiceImpl` | `listarBundlesVinculados` | Sí | SQL filter `organization_id`. |
| `ProductoServiceImpl` | `desvincularDeBundles` | Sí | Delete constrained by `organization_id`. |
| `ProveedorServiceImpl` | `listar` | Sí | Repo by `organizationId`. |
| `ProveedorServiceImpl` | `crear` | Sí | Saves entity with `organizationId`. |
| `ProveedorServiceImpl` | `actualizar` | Sí | Repo `findByIdAndOrganizationId`. |
| `ProveedorServiceImpl` | `archivar` | Sí | Repo `findByIdAndOrganizationId`. |
| `ProveedorServiceImpl` | `restaurar` | Sí | Repo `findByIdAndOrganizationId`. |
| `ProveedorServiceImpl` | `detalle` | Sí | Provider + products + PO aggregates filter by `organization_id`. |
| `StockAlertManager` | `syncForProduct` | Sí | Uses product `organizationId` for alert upsert/query. |
| `UsuarioAdminServiceImpl` | `listar` | Sí | Membership listing by `organizationId`. |
| `UsuarioAdminServiceImpl` | `invitar` | Sí | Requires owner/admin + creates membership in current org. |
| `UsuarioAdminServiceImpl` | `actualizarRol` | Sí | User resolved via membership and org check. |
| `UsuarioAdminServiceImpl` | `desactivar` | Sí | User resolved via membership and org check. |
| `UsuarioAdminServiceImpl` | `activar` | Sí | User resolved via membership and org check. |
| `UsuarioAdminServiceImpl` | `auditoria` | Sí | SQL joins filtered by `om.organization_id`. |
| `VentaServiceImpl` | `listar` | Sí | SQL filter `s.organization_id`. |
| `VentaServiceImpl` | `crear` | Sí | Client/product/bundle validations and writes constrained by `organization_id`. |

## Hallazgos

- No se detectaron fugas de datos entre organizaciones en los métodos públicos auditados.
- `AnaliticaController` `@PreAuthorize("isAuthenticated()")` is acceptable with current matrix because service-layer queries are org-scoped and membershipless users are blocked by `requireMembership()`.
