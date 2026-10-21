package br.com.aweb.sistema_vendas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/")
    public String home() {
        return "redirect:/pedidos";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Página de confirmação; o POST /logout fica no formulário dela
    @GetMapping("/logout")
    public String logout() {
        return "logout";
    }
}
