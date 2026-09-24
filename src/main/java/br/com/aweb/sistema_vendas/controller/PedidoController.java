package br.com.aweb.sistema_vendas.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.model.Pedido;
import br.com.aweb.sistema_vendas.model.StatusPedido;
import br.com.aweb.sistema_vendas.repository.ClienteRepository;
import br.com.aweb.sistema_vendas.repository.ProdutoRepository;
import br.com.aweb.sistema_vendas.service.PedidoService;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoController(
            PedidoService pedidoService,
            ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository) {

        this.pedidoService = pedidoService;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
    }

    @GetMapping
    public ModelAndView listarPedidos() {

        return new ModelAndView(
            "pedido/list",
            Map.of(
                "pedidos",
                pedidoService.listarTodos()
            )
        );
    }

    @GetMapping("/novo")
    public ModelAndView novoPedidoForm() {

        return new ModelAndView(
            "pedido/form",
            Map.of(
                "pedido",
                new Pedido(),
                "clientes",
                clienteRepository.findAll()
            )
        );
    }

    @PostMapping("/novo")
    public String criarPedido(
            @RequestParam Long clienteId,
            RedirectAttributes redirectAttributes) {

        try {

            Cliente cliente =
                clienteRepository.findById(clienteId)
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            "Cliente não encontrado."
                        )
                    );

            Pedido pedido =
                pedidoService.criarPedido(cliente);

            redirectAttributes.addFlashAttribute(
                "sucesso",
                "Pedido criado. Agora adicione os produtos."
            );

            return "redirect:/pedidos/edit/"
                    + pedido.getId();

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                "erro",
                e.getMessage()
            );

            return "redirect:/pedidos/novo";
        }
    }

    @GetMapping("/edit/{id}")
    public ModelAndView editarPedidoForm(
            @PathVariable Long id) {

        Pedido pedido =
            pedidoService.buscarPorId(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND
                    )
                );

        if (pedido.getStatus()
                == StatusPedido.CANCELADO) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Pedido cancelado não pode ser editado."
            );
        }

        return new ModelAndView(
            "pedido/edit",
            Map.of(
                "pedido",
                pedido,
                "produtos",
                produtoRepository.findAll()
            )
        );
    }

    @PostMapping("/{pedidoId}/adicionar-item")
    public String adicionarItem(
            @PathVariable Long pedidoId,
            @RequestParam Long produtoId,
            @RequestParam Integer quantidade,
            RedirectAttributes redirectAttributes) {

        try {

            pedidoService.adicionarItem(
                pedidoId,
                produtoId,
                quantidade
            );

            redirectAttributes.addFlashAttribute(
                "sucesso",
                "Item adicionado ao pedido."
            );

        } catch (
            IllegalArgumentException
            | IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                "erro",
                e.getMessage()
            );
        }

        return "redirect:/pedidos/edit/"
                + pedidoId;
    }

    @PostMapping(
        "/{pedidoId}/alterar-item/{itemId}"
    )
    public String alterarQuantidadeItem(
            @PathVariable Long pedidoId,
            @PathVariable Long itemId,
            @RequestParam Integer quantidade,
            RedirectAttributes redirectAttributes) {

        try {

            pedidoService.alterarQuantidadeItem(
                pedidoId,
                itemId,
                quantidade
            );

            redirectAttributes.addFlashAttribute(
                "sucesso",
                "Quantidade atualizada."
            );

        } catch (
            IllegalArgumentException
            | IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                "erro",
                e.getMessage()
            );
        }

        return "redirect:/pedidos/edit/"
                + pedidoId;
    }

    @PostMapping(
        "/{pedidoId}/remover-item/{itemId}"
    )
    public String removerItem(
            @PathVariable Long pedidoId,
            @PathVariable Long itemId,
            RedirectAttributes redirectAttributes) {

        try {

            pedidoService.removerItem(
                pedidoId,
                itemId
            );

            redirectAttributes.addFlashAttribute(
                "sucesso",
                "Item removido e estoque devolvido."
            );

        } catch (
            IllegalArgumentException
            | IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                "erro",
                e.getMessage()
            );
        }

        return "redirect:/pedidos/edit/"
                + pedidoId;
    }

    @PostMapping("/{id}/finalizar")
    public String finalizarPedido(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Pedido pedido =
            pedidoService.buscarPorId(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND
                    )
                );

        if (pedido.getItens().isEmpty()) {

            redirectAttributes.addFlashAttribute(
                "erro",
                "O pedido precisa ter pelo menos um produto."
            );

            return "redirect:/pedidos/edit/"
                    + id;
        }

        redirectAttributes.addFlashAttribute(
            "sucesso",
            "Pedido conferido e finalizado para a listagem."
        );

        return "redirect:/pedidos";
    }

    @GetMapping("/cancelar/{id}")
    public ModelAndView cancelarPedidoForm(
            @PathVariable Long id) {

        Pedido pedido =
            pedidoService.buscarPorId(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND
                    )
                );

        return new ModelAndView(
            "pedido/cancelar",
            Map.of("pedido", pedido)
        );
    }

    @PostMapping("/cancelar/{id}")
    public String cancelarPedido(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            pedidoService.cancelarPedido(id);

            redirectAttributes.addFlashAttribute(
                "sucesso",
                "Pedido cancelado e produtos devolvidos ao estoque."
            );

        } catch (
            IllegalArgumentException
            | IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                "erro",
                e.getMessage()
            );
        }

        return "redirect:/pedidos";
    }

    @GetMapping("/detalhes/{id}")
    public ModelAndView detalhesPedido(
            @PathVariable Long id) {

        Pedido pedido =
            pedidoService.buscarPorId(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND
                    )
                );

        return new ModelAndView(
            "pedido/detalhes",
            Map.of("pedido", pedido)
        );
    }
}