package br.com.sindico.app.security;

public final class PasswordPolicy {

    public static final String DEFAULT_MIN_LENGTH_MESSAGE = "Senha deve ter no minimo 8 caracteres.";
    public static final String PASSWORDS_DO_NOT_MATCH_MESSAGE = "As senhas nao conferem.";
    public static final String LETTER_AND_NUMBER_MESSAGE = "Senha deve conter letras e numeros.";

    private PasswordPolicy() {}

    public static void validateNewPassword(String password, String confirmation) {
        validateNewPassword(password, confirmation, DEFAULT_MIN_LENGTH_MESSAGE);
    }

    public static void validateNewPassword(String password, String confirmation, String minLengthMessage) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(minLengthMessage);
        }
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException(PASSWORDS_DO_NOT_MATCH_MESSAGE);
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException(LETTER_AND_NUMBER_MESSAGE);
        }
    }
}
