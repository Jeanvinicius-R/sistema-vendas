package br.com.aweb.sistema_vendas;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SistemaVendasApplication {

	public static void main(String[] args) {
		// O servidor (Render) roda em UTC; datas dos pedidos e filtros do relatório usam o horário de Brasília
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
		SpringApplication.run(SistemaVendasApplication.class, args);
	}

}
