package br.com.sindico.app.web;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handler global de exceções para todos os @RestController dentro de /api/**.
 * MELHORIA-006: Centraliza o tratamento de erro que estava duplicado em
 * ManutencaoApiController, GastoApiController, ReuniaoApiController, etc.
 * Qualquer controller pode ainda definir seu próprio @ExceptionHandler local
 * para sobrescrever este comportamento genérico.
 */
@RestControllerAdvice(annotations = RestController.class)
public class GlobalApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(EntityNotFoundException ex) {
        return Map.of("message", ex.getMessage(), "status", 404);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "Valor invalido" : fe.getDefaultMessage()))
                .toList();
        return Map.of("message", "Dados invalidos", "errors", errors, "status", 400);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBusinessRule(IllegalArgumentException ex) {
        return Map.of("message", ex.getMessage(), "status", 400);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleIllegalState(@SuppressWarnings("unused") IllegalStateException ex) {
        return Map.of("message", "Erro interno no servidor", "status", 500);
    }
}

