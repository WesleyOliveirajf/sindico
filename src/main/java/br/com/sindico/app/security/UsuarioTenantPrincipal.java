package br.com.sindico.app.security;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UsuarioTenantPrincipal implements UserDetails {

    private final UUID usuarioId;
    /** Primeiro condominio por ordenacao de nome (mesma regra dos vinculos). */
    private final UUID condominioPadraoId;

    private final Set<UUID> condominiosPermitidos;
    private final String email;
    private final String senhaHash;
    private final Collection<? extends GrantedAuthority> autoridades;

    public UsuarioTenantPrincipal(
            UUID usuarioId,
            UUID condominioPadraoId,
            Set<UUID> condominiosPermitidos,
            String email,
            String senhaHash,
            Collection<? extends GrantedAuthority> autoridades
    ) {
        this.usuarioId = usuarioId;
        this.condominioPadraoId = condominioPadraoId;
        this.condominiosPermitidos = Set.copyOf(condominiosPermitidos);
        this.email = email;
        this.senhaHash = senhaHash;
        this.autoridades = autoridades;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    /** Condominio padrao (ordenacao por nome); use {@link TenantAccessor#condominioAtual()} para o ativo na sessao. */
    public UUID getCondominioPadraoId() {
        return condominioPadraoId;
    }

    public Set<UUID> getCondominiosPermitidos() {
        return condominiosPermitidos;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return autoridades;
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
