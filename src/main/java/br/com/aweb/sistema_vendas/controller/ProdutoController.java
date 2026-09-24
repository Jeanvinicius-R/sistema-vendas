package br.com.aweb.sistema_vendas.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import br.com.aweb.sistema_vendas.model.Produto;
import br.com.aweb.sistema_vendas.service.ProdutoService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public ModelAndView list() {

        return new ModelAndView(
            "produto/list",
            Map.of(
                "produtos",
                produtoService.listarTodos()
            )
        );
    }

    @GetMapping("/novo")
    public ModelAndView create() {

        return new ModelAndView(
            "produto/form",
            Map.of(
                "produto",
                new Produto()
            )
        );
    }

    @PostMapping("/novo")
    public String create(
            @Valid Produto produto,
            BindingResult result) {

        if (result.hasErrors()) {
            return "produto/form";
        }

        produtoService.salvar(produto);

        return "redirect:/produtos";
    }

    @GetMapping("/edit/{id}")
    public ModelAndView edit(
            @PathVariable Long id) {

        var produto = produtoService
                .buscarPorId(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produto não encontrado."
                    )
                );

        return new ModelAndView(
            "produto/form",
            Map.of(
                "produto",
                produto
            )
        );
    }

    @PostMapping("/edit/{id}")
    public String edit(
            @PathVariable Long id,
            @Valid Produto produto,
            BindingResult result) {

        if (result.hasErrors()) {
            return "produto/form";
        }

        produtoService.atualizar(
            id,
            produto
        );

        return "redirect:/produtos";
    }

    @GetMapping("/delete/{id}")
    public ModelAndView deleteForm(
            @PathVariable Long id) {

        var produto = produtoService
                .buscarPorId(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produto não encontrado."
                    )
                );

        return new ModelAndView(
            "produto/delete",
            Map.of(
                "produto",
                produto
            )
        );
    }

    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id) {

        produtoService.excluir(id);

        return "redirect:/produtos";
    }
}