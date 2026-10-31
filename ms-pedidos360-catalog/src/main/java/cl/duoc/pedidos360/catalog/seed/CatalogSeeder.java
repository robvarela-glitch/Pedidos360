package cl.duoc.pedidos360.catalog.seed;

import cl.duoc.pedidos360.catalog.model.Product;
import cl.duoc.pedidos360.catalog.model.ProductSpecifications;
import cl.duoc.pedidos360.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Carga inicial del catálogo en Oracle, SOLO si la tabla está vacía.
 * (Reemplaza los productos que antes estaban escritos a mano en el frontend.)
 * Se desactiva con CATALOG_SEED=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pedidos360.catalog.seed", havingValue = "true", matchIfMissing = true)
public class CatalogSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }
        List<Product> products = List.of(
                product("TEC-001", "Logitech G915", "Logitech", "Teclados", "/products/logitech-g915.webp", 159990.0, 12,
                        "Teclado mecánico inalámbrico de alto rendimiento diseñado para gaming y productividad.",
                        spec("Mecánico", "Bluetooth / Wireless", "Negro", "2 años")),
                product("MON-002", "Samsung Odyssey G5", "Samsung", "Monitores", "/products/samsung-odyssey-g5.jpg", 279990.0, 8,
                        "Monitor gaming curvo diseñado para ofrecer una experiencia inmersiva y fluida.",
                        spec("Gaming Curvo", "HDMI / DisplayPort", "Negro", "1 año")),
                product("NOTE-003", "ASUS TUF Gaming", "ASUS", "Notebooks", "/products/asus-tuf-gaming.png", 899990.0, 5,
                        "Notebook gaming de alto rendimiento diseñado para videojuegos y tareas exigentes.",
                        spec("Notebook Gaming", "Wi-Fi / Bluetooth", "Negro", "1 año")),
                product("NOTE-004", "Lenovo ThinkPad", "Lenovo", "Notebooks", "/products/lenovo-thinkpad.avif", 699990.0, 15,
                        "Notebook empresarial orientado a productividad, movilidad y trabajo profesional.",
                        spec("Notebook Empresarial", "Wi-Fi / Bluetooth", "Negro", "1 año")),
                product("TEL-005", "Samsung Galaxy S26", "Samsung", "Smartphones", "/products/samsung-galaxy.jpg", 999990.0, 21,
                        "Smartphone Samsung de última generación con pantalla de alta calidad y gran rendimiento.",
                        spec("Smartphone", "5G / Wi-Fi / Bluetooth", "Violeta", "1 año")),
                product("AUD-006", "HyperX Cloud III", "HyperX", "Audio", "/products/hyperx-cloud-iii.jpg", 89990.0, 18,
                        "Audífonos gaming diseñados para ofrecer sonido envolvente y comodidad durante largas sesiones.",
                        spec("Audífonos Gaming", "USB / 3.5 mm", "Negro / Rojo", "2 años")),
                product("MOU-007", "Logitech MX Master", "Logitech", "Mouse", "/products/logitech-mx-master.jpg", 109990.0, 14,
                        "Mouse inalámbrico premium diseñado para productividad y trabajo profesional.",
                        spec("Mouse Inalámbrico", "Bluetooth / Wireless", "Negro", "2 años")));
        productRepository.saveAll(products);
        log.info("Catálogo inicial cargado en Oracle: {} productos", products.size());
    }

    private static Product product(String sku, String name, String brand, String category, String image,
                                   Double price, Integer stock, String description, ProductSpecifications specs) {
        return Product.builder().sku(sku).name(name).brand(brand).category(category).imageUrl(image)
                .price(price).stock(stock).description(description).specifications(specs).active(true).build();
    }

    private static ProductSpecifications spec(String type, String connectivity, String color, String warranty) {
        return ProductSpecifications.builder().type(type).connectivity(connectivity).color(color).warranty(warranty).build();
    }
}
