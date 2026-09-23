package br.com.aweb.sistema_vendas.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.service.ClienteService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ModelAndView list() {
        return new ModelAndView("cliente/list", Map.of("clientes", clienteService.listarTodos()));
    }

    @GetMapping("/novo")
    public ModelAndView create() {
        return new ModelAndView("cliente/form", Map.of("cliente", new Cliente()));
    }

    @PostMapping("/novo")
    public String create(@Valid Cliente cliente, BindingResult result) {
        validarUnicidade(cliente, result);

        if (result.hasErrors()) {
            return "cliente/form";
        }

        clienteService.salvar(cliente);
        return "redirect:/clientes";
    }

    @GetMapping("/edit/{id}")
    public ModelAndView edit(@PathVariable Long id) {
        var optionalCliente = clienteService.buscarPorId(id);
        if (optionalCliente.isPresent()) {
            return new ModelAndView("cliente/form", Map.of("cliente", optionalCliente.get()));
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @Valid Cliente cliente, BindingResult result) {
        cliente.setId(id);
        validarUnicidade(cliente, result);

        if (result.hasErrors()) {
            return "cliente/form";
        }

        clienteService.atualizar(id, cliente);
        return "redirect:/clientes";
    }

    @GetMapping("/delete/{id}")
    public ModelAndView delete(@PathVariable Long id) {
        var optionalCliente = clienteService.buscarPorId(id);
        if (optionalCliente.isPresent()) {
            return new ModelAndView("cliente/delete", Map.of("cliente", optionalCliente.get()));
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/delete/{id}")
    public String delete(Cliente cliente) {
        clienteService.excluir(cliente.getId());
        return "redirect:/clientes";
    }

    private void validarUnicidade(Cliente cliente, BindingResult result) {
        if (cliente.getEmail() != null
                && clienteService.emailPertenceAOutroCliente(cliente.getEmail(), cliente.getId())) {
            result.rejectValue("email", "email.duplicado", "Este e-mail já está cadastrado");
        }

        if (cliente.getCpf() != null
                && clienteService.cpfPertenceAOutroCliente(cliente.getCpf(), cliente.getId())) {
            result.rejectValue("cpf", "cpf.duplicado", "Este CPF já está cadastrado");
        }
    }

}