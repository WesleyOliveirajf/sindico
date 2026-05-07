package br.com.sindico.app.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Um condominio na sessao por vez. Multiplos acessos: vai para tela de escolha apos login.
 */
public class SindicoLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public SindicoLoginSuccessHandler() {
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {
        if (!(authentication.getPrincipal() instanceof UsuarioTenantPrincipal principal)) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        HttpSession session = request.getSession();
        session.removeAttribute(TenantSession.CONDOMINIO_ID);

        Set<UUID> permitidos = principal.getCondominiosPermitidos();
        if (permitidos.size() <= 1) {
            session.setAttribute(TenantSession.CONDOMINIO_ID, principal.getCondominioPadraoId());
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, "/condominios/selecionar");
    }
}
