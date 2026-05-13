package com.nuv.tfgrs.entity;
 
import com.nuv.tfgrs.security.TokenVersionAware;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
 
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
 
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements UserDetails, TokenVersionAware {

    public static final String DEFAULT_TIME_ZONE = "Europe/Madrid";
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(length = 50)
    private String nombre;

    @Column(length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 50)
    private String username;
 
    @Column(nullable = false, unique = true, length = 255)
    private String email;
 
    @Column(name = "password_hash", nullable = true)
    private String passwordHash;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean active;
 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
 
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    // Also make password nullable:
    //@Column(nullable = true)
    //private String password;
 
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        if (active == null) {
            active = true;
        }
        if (mustChangePassword == null) {
            mustChangePassword = false;
        }
        if (tokenVersion == null) {
            tokenVersion = 0;
        }
        if (timeZone == null || timeZone.isBlank()) {
            timeZone = DEFAULT_TIME_ZONE;
        }
    }
 
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
 
    // --- UserDetails implementation ---
 
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }
 
    @Override
    public String getPassword() {
        return passwordHash;
    }
 
    @Override
    public boolean isAccountNonExpired()  { return true; }
 
    @Override
    public boolean isAccountNonLocked()   { return true; }
 
    @Override
    public boolean isCredentialsNonExpired() { return true; }
 
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(active);
    }
 
    public enum Role {
        ROLE_ADMIN,
        ROLE_MANAGER,
        ROLE_VIEWER
    }
}
 
