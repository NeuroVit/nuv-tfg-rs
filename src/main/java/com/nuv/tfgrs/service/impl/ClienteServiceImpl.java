package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.request.CrearClienteRequest;
import com.nuv.tfgrs.dto.request.EditarClienteRequest;
import com.nuv.tfgrs.dto.response.ClienteResponse;
import com.nuv.tfgrs.entity.Cliente;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.repository.ClienteRepository;
import com.nuv.tfgrs.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(boolean includeArchived) {
        List<Cliente> clientes = includeArchived
                ? clienteRepository.findAllByOrderByNameAsc()
                : clienteRepository.findByActiveTrueOrderByNameAsc();

        return clientes.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ClienteResponse crear(CrearClienteRequest request) {
        Cliente cliente = Cliente.builder()
                .name(normalizeRequired(request.nombre(), "El nombre es obligatorio"))
                .email(normalizeEmail(request.email(), null))
                .phone(normalizeOptional(request.telefono()))
                .notes(normalizeOptional(request.notas()))
                .active(true)
                .build();

        return toResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteResponse actualizar(Long id, EditarClienteRequest request) {
        Cliente cliente = findOrThrow(id);
        cliente.setName(normalizeRequired(request.nombre(), "El nombre es obligatorio"));
        cliente.setEmail(normalizeEmail(request.email(), id));
        cliente.setPhone(normalizeOptional(request.telefono()));
        cliente.setNotes(normalizeOptional(request.notas()));
        return toResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteResponse archivar(Long id) {
        Cliente cliente = findOrThrow(id);
        cliente.setActive(false);
        return toResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteResponse restaurar(Long id) {
        Cliente cliente = findOrThrow(id);
        cliente.setActive(true);
        return toResponse(clienteRepository.save(cliente));
    }

    private Cliente findOrThrow(Long id) {
        return clienteRepository.findById(id)
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

    private String normalizeEmail(String email, Long currentId) {
        String normalized = normalizeOptional(email);
        if (normalized == null) {
            return null;
        }

        String lowercase = normalized.toLowerCase();
        if (currentId == null) {
            if (clienteRepository.findByEmail(lowercase).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese email");
            }
        } else if (clienteRepository.existsByEmailAndIdNot(lowercase, currentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese email");
        }

        return lowercase;
    }
}
