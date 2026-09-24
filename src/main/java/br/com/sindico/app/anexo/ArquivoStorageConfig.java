package br.com.sindico.app.anexo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ArquivoStorageConfig {

    @Bean
    ArquivoStorage arquivoStorage(
            @Value("${app.storage.provider:local}") String provider,
            @Value("${app.storage.upload-dir:uploads}") String uploadDir,
            @Value("${app.storage.supabase.url:}") String supabaseUrl,
            @Value("${app.storage.supabase.service-key:}") String serviceKey,
            @Value("${app.storage.supabase.bucket:anexos}") String bucket) {
        return switch (provider.trim().toLowerCase()) {
            case "local" -> new LocalArquivoStorage(uploadDir);
            case "supabase" -> new SupabaseArquivoStorage(supabaseUrl, serviceKey, bucket);
            default -> throw new IllegalStateException("app.storage.provider invalido: " + provider + " (use local ou supabase)");
        };
    }
}
