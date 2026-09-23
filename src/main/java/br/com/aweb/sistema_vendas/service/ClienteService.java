package br.com.aweb.sistema_vendas.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.repository.ClienteRepository;
import jakarta.transaction.Transactional;

@Service
public class ClienteService {

    @Autowired
    ClienteRepository clienteRepository;

    // CREATE
    @Transactional
    public Cliente salvar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    // READ
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    // UPDATE
    @Transactional
    public Cliente atualizar(Long id, Cliente clienteAtualizado) {
        var optionalCliente = buscarPorId(id);
        if (!optionalCliente.isPresent())
            throw new IllegalArgumentException("Cliente não encontrado.");

        var clienteExistente = optionalCliente.get();

        clienteExistente.setNomeCompleto(clienteAtualizado.getNomeCompleto());
        clienteExistente.setEmail(clienteAtualizado.getEmail());
        clienteExistente.setCpf(clienteAtualizado.getCpf());
        clienteExistente.setTelefone(clienteAtualizado.getTelefone());
        clienteExistente.setEndereco(clienteAtualizado.getEndereco());

        return clienteRepository.save(clienteExistente);
    }

    // DELETE
    @Transactional
    public void excluir(Long id) {
        var optionalCliente = buscarPorId(id);
        if (!optionalCliente.isPresent())
            throw new IllegalArgumentException("Cliente não encontrado.");

        clienteRepository.deleteById(id);
    }

    // Validações de unicidade (usadas pelo controller antes de salvar/atualizar)

    /**
     * Retorna true se já existe outro cliente (id diferente do informado)
     * cadastrado com o e-mail informado.
     */
    public boolean emailPertenceAOutroCliente(String email, Long idAtual) {
        return clienteRepository.findByEmail(email)
                .map(Cliente::getId)
                .map(id -> !id.equals(idAtual))
                .orElse(false);
    }

    /**
     * Retorna true se já existe outro cliente (id diferente do informado)
     * cadastrado com o CPF informado.
     */
    public boolean cpfPertenceAOutroCliente(String cpf, Long idAtual) {
        return clienteRepository.findByCpf(cpf)
                .map(Cliente::getId)
                .map(id -> !id.equals(idAtual))
                .orElse(false);
    }

}
