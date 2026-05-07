package br.com.sindico.app.web;

import br.com.sindico.app.security.UsuarioTenantPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class TenantUiModelAdvice {

    @ModelAttribute("podeTrocarCondominio")
    public boolean podeTrocarCondominio() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        if (!(auth.getPrincipal() instanceof UsuarioTenantPrincipal principal)) {
            return false;
        }
        return principal.getCondominiosPermitidos().size() > 1;
    }
}
