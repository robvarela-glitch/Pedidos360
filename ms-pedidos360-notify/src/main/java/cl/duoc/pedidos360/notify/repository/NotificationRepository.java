package cl.duoc.pedidos360.notify.repository;

import cl.duoc.pedidos360.notify.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByCustomerId(String customerId);
    List<Notification> findByOrderId(Long orderId);
}
