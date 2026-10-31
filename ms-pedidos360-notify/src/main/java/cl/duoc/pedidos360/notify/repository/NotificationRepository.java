package cl.duoc.pedidos360.notify.repository;

import cl.duoc.pedidos360.notify.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByOrderBySentAtDesc();
    List<Notification> findByCustomerIdOrderBySentAtDesc(String customerId);
    List<Notification> findByOrderIdOrderBySentAtDesc(Long orderId);
    List<Notification> findByOrderIdAndCustomerIdOrderBySentAtDesc(Long orderId, String customerId);
}
