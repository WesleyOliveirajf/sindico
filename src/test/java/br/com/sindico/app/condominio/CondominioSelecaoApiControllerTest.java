package br.com.sindico.app.condominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import br.com.sindico.app.security.UsuarioTenantPrincipal;

class CondominioSelecaoApiControllerTest {

    @Test
    void listaSomenteCondominiosPermitidosOrdenadosPorNome() {
        CondominioRepository repository = mock(CondominioRepository.class);
        UUID idB = UUID.randomUUID();
        UUID idA = UUID.randomUUID();
        when(repository.findAllById(anyIterable())).thenReturn(List.of(
                condominio(idB, "Bela Vista"),
                condominio(idA, "Alfa")));

        UsuarioTenantPrincipal principal = new UsuarioTenantPrincipal(
                UUID.randomUUID(), idA, Set.of(idA, idB), "sindico@email.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        List<CondominioSelecaoApiController.CondominioOpcao> opcoes =
                new CondominioSelecaoApiController(repository).listar(principal);

        assertThat(opcoes).extracting(CondominioSelecaoApiController.CondominioOpcao::nome)
                .containsExactly("Alfa", "Bela Vista");
    }

    @Test
    void semPrincipalRetornaListaVazia() {
        CondominioRepository repository = mock(CondominioRepository.class);

        assertThat(new CondominioSelecaoApiController(repository).listar(null)).isEmpty();
    }

    private static Condominio condominio(UUID id, String nome) {
        Condominio condominio = new Condominio();
        ReflectionTestUtils.setField(condominio, "id", id);
        condominio.setNome(nome);
        return condominio;
    }
}
