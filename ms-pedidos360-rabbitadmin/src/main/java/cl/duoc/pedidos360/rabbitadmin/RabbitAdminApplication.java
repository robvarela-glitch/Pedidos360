package cl.duoc.pedidos360.rabbitadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio administrador de RabbitMQ de Pedidos360.
 * Expone una API REST (protegida con JWT, rol Admin) para crear y eliminar
 * colas, exchanges y bindings, y para consultar el estado del broker/clúster.
 */
@SpringBootApplication
public class RabbitAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(RabbitAdminApplication.class, args);
    }
}
