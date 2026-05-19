package br.com.sindico.app.admin;

import br.com.sindico.app.condominio.Condominio;
import br.com.sindico.app.condominio.CondominioRepository;
import br.com.sindico.app.usuario.Usuario;
import br.com.sindico.app.usuario.UsuarioCondominio;
import br.com.sindico.app.usuario.UsuarioCondominioRepository;
import br.com.sindico.app.usuario.UsuarioRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o usuario admin master na primeira inicializacao da aplicacao.
 * Requer as variaveis de ambiente APP_ADMIN_EMAIL e APP_ADMIN_PASSWORD.
 * Se o email ja existir no banco, nenhuma acao e tomada (idempotente).
 */
@Service
public class AdminInitializerService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializerService.class);

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    private final UsuarioRepository usuarioRepository;
    private final CondominioRepository condominioRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializerService(
            UsuarioRepository usuarioRepository,
            CondominioRepository condominioRepository,
            UsuarioCondominioRepository usuarioCondominioRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.condominioRepository = condominioRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.warn("[Admin] APP_ADMIN_EMAIL ou APP_ADMIN_PASSWORD nao configurados. Admin master nao sera criado.");
            return;
        }

        String emailNorm = adminEmail.trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByEmailNormalizado(emailNorm)) {
            log.info("[Admin] Admin master ja registrado: {}", emailNorm);
            return;
        }

        // Cria condominio reservado para o admin
        Condominio sistemaCondominio = new Condominio();
        sistemaCondominio.setNome("Sistema Admin");
        sistemaCondominio = condominioRepository.save(sistemaCondominio);

        // Cria usuario admin
        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setEmail(emailNorm);
        admin.setSenhaHash(passwordEncoder.encode(adminPassword));
        admin.setStatus("ativo");
        admin = usuarioRepository.save(admin);

        // Vincula ao condominio sistema com perfil ADMIN
        UsuarioCondominio vinculo = new UsuarioCondominio();
        vinculo.setUsuario(admin);
        vinculo.setCondominio(sistemaCondominio);
        vinculo.setPerfil("ADMIN");
        usuarioCondominioRepository.save(vinculo);

        log.info("[Admin] Admin master criado com sucesso: {}", emailNorm);
    }
}
