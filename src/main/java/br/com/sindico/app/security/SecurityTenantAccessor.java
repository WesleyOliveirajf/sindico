package br.com.sindico.app.security;

import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SecurityTenantAccessor implements TenantAccessor {

    public static final String CONDOMINIO_HEADER = "X-Condominio-Id";

    private static UUID condominioDoCabecalho(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valor.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public UUID condominioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Nao autenticado");
        }
        if (!(auth.getPrincipal() instanceof UsuarioTenantPrincipal usuario)) {
            throw new IllegalStateException("Tipo de autenticacao nao suportado");
        }

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            // Clientes JWT (SPA) escolhem o condominio por cabecalho; so vale se estiver entre os permitidos.
            UUID doCabecalho = condominioDoCabecalho(attrs.getRequest().getHeader(CONDOMINIO_HEADER));
            if (doCabecalho != null && usuario.getCondominiosPermitidos().contains(doCabecalho)) {
                return doCabecalho;
            }

            HttpSession session = attrs.getRequest().getSession(false);
            if (session != null) {
                Object raw = session.getAttribute(TenantSession.CONDOMINIO_ID);
                if (raw instanceof UUID selecionado && usuario.getCondominiosPermitidos().contains(selecionado)) {
                    return selecionado;
                }
            }
        }

        return usuario.getCondominioPadraoId();
    }
}
