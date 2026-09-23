package cl.duoc.pedidos360.notify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PED360_NOTIFICATIONS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private String customerId;

    @Column(name = "EVENT_TYPE", nullable = false)
    private String eventType;

    @Column(name = "MESSAGE", length = 500)
    private String message;

    @Column(name = "SENT_AT", nullable = false)
    private LocalDateTime sentAt;
}
