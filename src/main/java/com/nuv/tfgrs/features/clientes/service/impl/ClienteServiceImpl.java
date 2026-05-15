package com.nuv.tfgrs.features.clientes.service.impl;

import com.nuv.tfgrs.features.clientes.dto.request.CrearClienteRequest;
import com.nuv.tfgrs.features.clientes.dto.request.EditarClienteRequest;
import com.nuv.tfgrs.features.clientes.dto.response.ClienteResponse;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.clientes.entity.Cliente;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.features.clientes.repository.ClienteRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.features.clientes.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CurrentOrganizationResolver currentOrganizationResolver;

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(boolean includeArchived) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        List<Cliente> clientes = includeArchived
                ? clienteRepository.findByOrganizationIdOrderByNameAsc(organizationId)
                : clienteRepository.findByOrganizationIdAndActiveTrueOrderByNameAsc(organizationId);

        return clientes.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ClienteResponse> listarPaginado(String status, String search, Integer limit, Integer offset) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 100));
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        int pageIndex = safeOffset / safeLimit;

        String normalizedStatus = normalizeStatus(status);
        String normalizedSearch = normalizeLikeSearch(search);

        Page<Cliente> page = clienteRepository.findPagedByFilters(
                organizationId,
                normalizedStatus,
                normalizedSearch,
                PageRequest.of(pageIndex, safeLimit)
        );

        List<ClienteResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new PagedResponse<>(content, page.getTotalElements(), safeLimit, safeOffset);
    }

    @Override
    @Transactional
    public ClienteResponse crear(CrearClienteRequest request) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Cliente cliente = Cliente.builder()
                .organizationId(organizationId)
                .name(normalizeRequired(request.nombre(), "El nombre es obligatorio"))
                .email(normalizeEmail(organizationId, request.email(), null))
                .phone(normalizeOptional(request.telefono()))
                .notes(normalizeOptional(request.notas()))
                .active(true)
                .build();

        return toResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteResponse actualizar(Long id, EditarClienteRequest request) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Cliente cliente = findOrThrow(id, organizationId);
        cliente.setName(normalizeRequired(request.nombre(), "El nombre es obligatorio"));
        cliente.setEmail(normalizeEmail(organizationId, request.email(), id));
        cliente.setPhone(normalizeOptional(request.telefono()));
        cliente.setNotes(normalizeOptional(request.notas()));
        return toResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteResponse archivar(Long id) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Cliente cliente = findOrThrow(id, organizationId);
        cliente.setActive(false);
        return toResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteResponse restaurar(Long id) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Cliente cliente = findOrThrow(id, organizationId);
        cliente.setActive(true);
        return toResponse(clienteRepository.save(cliente));
    }

    private Cliente findOrThrow(Long id, Long organizationId) {
        return clienteRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    private ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getName(),
                cliente.getEmail(),
                cliente.getPhone(),
                cliente.getNotes(),
                Boolean.TRUE.equals(cliente.getActive()),
                cliente.getCreatedAt(),
                cliente.getUpdatedAt()
        );
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(Long organizationId, String email, Long currentId) {
        String normalized = normalizeOptional(email);
        if (normalized == null) {
            return null;
        }

        String lowercase = normalized.toLowerCase();
        if (currentId == null) {
            if (clienteRepository.findByOrganizationIdAndEmail(organizationId, lowercase).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese email");
            }
        } else if (clienteRepository.existsByOrganizationIdAndEmailAndIdNot(organizationId, lowercase, currentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese email");
        }

        return lowercase;
    }

    private String normalizeLikeSearch(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return null;
        return "%" + normalized + "%";
    }

    private String normalizeStatus(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> "ACTIVE";
            case "ARCHIVED" -> "ARCHIVED";
            default -> null;
        };
    }
}
