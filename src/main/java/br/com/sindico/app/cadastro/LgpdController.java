package br.com.sindico.app.cadastro;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LgpdController {

    @GetMapping("/termos")
    public String termos() {
        return "termos";
    }

    @GetMapping("/privacidade")
    public String privacidade() {
        return "privacidade";
    }

    @GetMapping("/cookies")
    public String cookies() {
        return "cookies";
    }
}
