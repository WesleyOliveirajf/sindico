package br.com.sindico.app;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Teste de integração que requer conexão com banco de dados. Execute apenas com infra disponível.")
class SindicoApplicationTests {

    @Test
    void contextLoads() {
    }
}