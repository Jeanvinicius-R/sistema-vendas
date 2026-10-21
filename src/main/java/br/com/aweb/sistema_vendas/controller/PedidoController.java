package br.com.aweb.sistema_vendas.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import br.com.aweb.sistema_vendas.service.RelatorioPedidoService;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final RelatorioPedidoService relatorioService;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoController(
            PedidoService pedidoService,
            RelatorioPedidoService relatorioService,
            ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository) {

        this.pedidoService = pedidoService;
        this.relatorioService = relatorioService;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
    }

    @GetMapping
    public ModelAndView listarPedidos() {

        List<Pedido> pedidos = pedidoService.listarTodos();

        return new ModelAndView(
            "pedido/list",
            Map.of(
                "pedidos",
                pedidos,
                "resumo",
                relatorioService.resumir(pedidos)
            )
        );
    }

    // RELATÓRIO DE PEDIDOS (filtros opcionais)
    @GetMapping("/relatorio")
    public ModelAndView relatorio(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) Long clienteId) {

        ModelAndView mv = new ModelAndView("pedido/relatorio");

        List<Pedido> pedidos = List.of();

        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            mv.addObject(
                "erro",
                "A data inicial não pode ser posterior à data final."
            );
        } else {
            pedidos = relatorioService.filtrar(
                inicio, fim, status, clienteId
            );
        }

        mv.addObject("pedidos", pedidos);
        mv.addObject("resumo", relatorioService.resumir(pedidos));
        mv.addObject("clientes", clienteRepository.findAll());
        mv.addObject("statusLista", StatusPedido.values());
        mv.addObject("inicio", inicio);
        mv.addObject("fim", fim);
        mv.addObject("status", status);
        mv.addObject("clienteId", clienteId);
        mv.addObject("geradoEm", LocalDateTime.now());

        return mv;
    }

    // RELATÓRIO DE PEDIDOS - exportação em CSV (mesmos filtros)
    @GetMapping("/relatorio/csv")
    public ResponseEntity<byte[]> relatorioCsv(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) Long clienteId) {

        List<Pedido> pedidos =
            relatorioService.filtrar(inicio, fim, status, clienteId);

        byte[] conteudo = relatorioService
                .gerarCsv(pedidos)
                .getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"relatorio-pedidos-"
                        + LocalDate.now() + ".csv\""
                )
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(conteudo);
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