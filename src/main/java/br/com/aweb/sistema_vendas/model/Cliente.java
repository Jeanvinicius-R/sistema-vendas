package br.com.aweb.sistema_vendas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.br.CPF;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome completo é obrigatório")
    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank(message = "CPF é obrigatório")
    @CPF(message = "CPF inválido")
    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @NotBlank(message = "Telefone é obrigatório")
    @Column(nullable = false, length = 20)
    private String telefone;

    @Valid
    @Embedded
    private Endereco endereco = new Endereco();

}
