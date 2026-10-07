package com.bookworm.ebookstore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.bookworm.ebookstore.config.StoreProperties;
import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.entity.Order;
import com.bookworm.ebookstore.entity.OrderItem;
import com.bookworm.ebookstore.entity.User;

/** Entities, repositories and config line up with the schema (AC-5); writes roll back. */
@SpringBootTest
@Transactional
class EntityMappingTest {

    @Autowired
    private BookRepository books;

    @Autowired
    private UserRepository users;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private StoreProperties store;

    @Autowired
    private Clock clock;

    @Test
    void seededBookLoadsWithGenresAndCatalogueLinks() {
        Book dracula = books.findAll().stream()
                .filter(b -> b.getTitle().equals("Dracula"))
                .findFirst().orElseThrow();

        assertThat(dracula.getFormat()).isEqualTo(BookFormat.PAPERBACK);
        assertThat(dracula.getPrice()).isEqualByComparingTo("425.00");
        assertThat(dracula.getAuthor().getName()).isEqualTo("Bram Stoker");
        assertThat(dracula.getCategory().getSlug()).isEqualTo("fantasy");
        assertThat(dracula.getGenres()).extracting("name").contains("Horror", "Classic");
        assertThat(dracula.isInStock()).isTrue();
    }

    @Test
    void orderWithItemsPersistsThroughJpa() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        User user = new User();
        user.setEmail("mapping@example.com");
        user.setPasswordHash("hash");
        user.setFirstName("Map");
        user.setLastName("Ping");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        users.save(user);

        Book book = books.findAll().get(0);
        OrderItem item = new OrderItem();
        item.setBook(book);
        item.setTitle(book.getTitle());
        item.setFormat(book.getFormat());
        item.setUnitPrice(book.getPrice());
        item.setQuantity(1);
        item.setLineTotal(book.getPrice());

        Order order = new Order();
        order.setOrderNumber("BW-MAPPING-" + orders.nextOrderNumber());
        order.setUser(user);
        order.setShipFirstName("Map");
        order.setShipLastName("Ping");
        order.setShipEmail("mapping@example.com");
        order.setShipPhone("+639171234567");
        order.setShipStreetAddress("1 Rizal St");
        order.setShipCity("Makati");
        order.setShipProvince("Metro Manila");
        order.setShipZipCode("1200");
        order.setSubtotal(book.getPrice());
        order.setTotalAmount(book.getPrice());
        order.setEstimatedDeliveryDate(LocalDate.now(clock));
        order.setPlacedAt(now);
        order.setUpdatedAt(now);
        order.addItem(item);

        orders.saveAndFlush(order);

        assertThat(order.getId()).isNotNull();
        assertThat(item.getId()).isNotNull();
        assertThat(user.getGiftPointsBalance()).isZero();
    }

    @Test
    void orderNumberSequenceIncreases() {
        long first = orders.nextOrderNumber();
        assertThat(orders.nextOrderNumber()).isGreaterThan(first);
    }

    @Test
    void storePropertiesBindFromConfig() {
        assertThat(store.currency()).isEqualTo("PHP");
        assertThat(store.zone().getId()).isEqualTo("Asia/Manila");
        assertThat(store.vatRate()).isEqualByComparingTo(new BigDecimal("0.12"));
        assertThat(store.deliveryDays()).isEqualTo(5);
        assertThat(store.points().pesosPerPoint()).isEqualTo(100);
        assertThat(store.points().pesoValue()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(store.cancelWindowHours()).isEqualTo(48);
    }
}
