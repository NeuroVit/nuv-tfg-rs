package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.response.BillingStatusResponse;
import com.nuv.tfgrs.entity.Usuario;
import com.nuv.tfgrs.repository.UsuarioRepository;
import com.nuv.tfgrs.service.BillingService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.Mode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final UsuarioRepository usuarioRepository;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.price-id}")
    private String priceId;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Override
    @Transactional(readOnly = true)
    public BillingStatusResponse getStatus(String username) {
        Usuario user = getUser(username);
        return BillingStatusResponse.builder()
                .status(user.getSubscriptionStatus())
                .subscriptionId(user.getSubscriptionId())
                .hasActiveSubscription("ACTIVE".equals(user.getSubscriptionStatus()))
                .build();
    }

    @Override
    @Transactional
    public String createCheckoutSession(String username, String successUrl, String cancelUrl) {
        try {
            Usuario user = getUser(username);
            String customerId = ensureCustomer(user);

            com.stripe.param.checkout.SessionCreateParams params =
                    com.stripe.param.checkout.SessionCreateParams.builder()
                            .setMode(Mode.SUBSCRIPTION)
                            .setCustomer(customerId)
                            .addLineItem(LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build())
                            .setSuccessUrl(successUrl)
                            .setCancelUrl(cancelUrl)
                            .build();

            com.stripe.model.checkout.Session session =
                    com.stripe.model.checkout.Session.create(params);
            return session.getUrl();
        } catch (Exception e) {
            log.error("Error creating Stripe checkout session", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo crear la sesión de pago");
        }
    }

    @Override
    @Transactional
    public String createPortalSession(String username, String returnUrl) {
        Usuario user = getUser(username);
        if (user.getStripeCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No tienes ninguna suscripción activa");
        }
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setCustomer(user.getStripeCustomerId())
                    .setReturnUrl(returnUrl)
                    .build();

            com.stripe.model.billingportal.Session session =
                    com.stripe.model.billingportal.Session.create(params);
            return session.getUrl();
        } catch (Exception e) {
            log.error("Error creating Stripe portal session", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo abrir el portal de facturación");
        }
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firma de webhook inválida");
        }

        switch (event.getType()) {
            case "customer.subscription.created", "customer.subscription.updated" -> {
                event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                    Subscription sub = (Subscription) obj;
                    updateSubscription(sub.getCustomer(), sub.getId(), mapStatus(sub.getStatus()));
                });
            }
            case "customer.subscription.deleted" -> {
                event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                    Subscription sub = (Subscription) obj;
                    updateSubscription(sub.getCustomer(), null, "CANCELLED");
                });
            }
            default -> log.debug("Stripe event ignored: {}", event.getType());
        }
    }

    private void updateSubscription(String customerId, String subscriptionId, String status) {
        usuarioRepository.findByStripeCustomerId(customerId).ifPresentOrElse(user -> {
            user.setSubscriptionId(subscriptionId);
            user.setSubscriptionStatus(status);
            usuarioRepository.save(user);
            log.info("Billing updated for user {}: status={}", user.getUsername(), status);
        }, () -> log.warn("No user found for Stripe customer {}", customerId));
    }

    private String ensureCustomer(Usuario user) throws Exception {
        if (user.getStripeCustomerId() != null) {
            return user.getStripeCustomerId();
        }
        String name = user.getNombre() != null && !user.getNombre().isBlank()
                ? user.getNombre() : user.getUsername();
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(name)
                .build();
        Customer customer = Customer.create(params);
        user.setStripeCustomerId(customer.getId());
        usuarioRepository.save(user);
        return customer.getId();
    }

    private String mapStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active", "trialing" -> "ACTIVE";
            case "past_due", "unpaid"  -> "PAST_DUE";
            default                    -> "INACTIVE";
        };
    }

    private Usuario getUser(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"));
    }
}
