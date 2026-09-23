package br.com.aweb.sistema_vendas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Endereco {

    @NotBlank(message = "Logradouro é obrigatório")
    @Column(nullable = false, length = 150)
    private String logradouro;

    // Opcional
    @Column(length = 20)
    private String numero;

    // Opcional
    @Column(length = 100)
    private String complemento;

    @NotBlank(message = "Bairro é obrigatório")
    @Column(nullable = false, length = 100)
    private String bairro;

    @NotBlank(message = "Cidade é obrigatória")
    @Column(nullable = false, length = 100)
    private String cidade;

    @NotBlank(message = "UF é obrigatória")
    @Pattern(regexp = "[A-Za-z]{2}", message = "UF deve conter 2 letras (ex: SP)")
    @Column(nullable = false, length = 2)
    private String uf;

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido. Use o formato 00000-000")
    @Column(nullable = false, length = 9)
    private String cep;

}
