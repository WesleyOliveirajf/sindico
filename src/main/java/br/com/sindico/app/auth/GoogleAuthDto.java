package br.com.sindico.app.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GoogleAuthDto(
    @NotBlank String credentialToken,
    @NotNull Boolean aceitouTermos,
    @NotNull Boolean aceitouMarketing
) {}
