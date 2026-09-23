package br.com.aweb.sistema_vendas.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.aweb.sistema_vendas.model.Pedido;
import br.com.aweb.sistema_vendas.model.StatusPedido;
import br.com.aweb.sistema_vendas.repository.ClienteRepository;
import br.com.aweb.sistema_vendas.repository.ProdutoRepository;
import br.com.aweb.sistema_vendas.service.PedidoService;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @GetMapping
    public ModelAndView list() {
        return new ModelAndView("pedido/list", Map.of("pedidos", pedidoService.listarTodos()));
    }

    @GetMapping("/novo")
    public String create(Model model) {
        model.addAttribute("pedido", new Pedido());
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("produtos", produtoRepository.findAll());
        return "pedido/form";
    }

    @PostMapping("/novo")
    public String create(@ModelAttribute Pedido pedido, Model model, RedirectAttributes redirectAttributes) {
        try {
            pedidoService.criar(pedido);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido registrado com sucesso.");
            return "redirect:/pedidos";
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("pedido", pedido);
            model.addAttribute("clientes", clienteRepository.findAll());
            model.addAttribute("produtos", produtoRepository.findAll());
            return "pedido/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        var pedido = pedidoService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (pedido.getStatus() != StatusPedido.ATIVO) {
            return "redirect:/pedidos";
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("produtos", produtoRepository.findAll());
        return "pedido/form";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute Pedido pedido, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            pedidoService.atualizar(id, pedido);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido atualizado com sucesso.");
            return "redirect:/pedidos";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("erro", e.getMessage());
            pedido.setId(id);
            model.addAttribute("pedido", pedido);
            model.addAttribute("clientes", clienteRepository.findAll());
            model.addAttribute("produtos", produtoRepository.findAll());
            return "pedido/form";
        }
    }

    @GetMapping("/cancelar/{id}")
    public ModelAndView cancelar(@PathVariable Long id) {
        var pedido = pedidoService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new ModelAndView("pedido/cancelar", Map.of("pedido", pedido));
    }

    @PostMapping("/cancelar/{id}")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            pedidoService.cancelar(id);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido cancelado e produtos devolvidos ao estoque.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos";
    }

}