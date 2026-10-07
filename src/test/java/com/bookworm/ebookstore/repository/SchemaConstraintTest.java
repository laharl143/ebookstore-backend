package com.bookworm.ebookstore.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * The database itself rejects bad data, each case through its named constraint (AC-4),
 * and no table can hold full card or wallet details (AC-6). Each test rolls back.
 */
@SpringBootTest
@Transactional
class SchemaConstraintTest {

    private static final String USER = "(SELECT id FROM users WHERE email = 'fixture@example.com')";
    private static final String BOOK = "(SELECT id FROM books WHERE title = 'Dracula' AND format = 'PAPERBACK')";
    private static final String ORDER = "(SELECT id FROM orders WHERE order_number = 'BW-TEST-1')";
    private static final String NOW = "CURRENT_TIMESTAMP";

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void insertFixtures() {
        jdbc.update(userInsert("fixture@example.com", "CUSTOMER"));
        jdbc.update(orderInsert("BW-TEST-1"));
        jdbc.update("INSERT INTO cart_items (user_id, book_id, quantity, created_at, updated_at) VALUES ("
                + USER + ", " + BOOK + ", 1, " + NOW + ", " + NOW + ")");
        jdbc.update(paymentInsert("CREDIT_CARD", "'4242'", "TXN-1", "SUCCESS"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("violations")
    void badDataIsRejectedByNamedConstraint(String constraint, String sql) {
        assertThatThrownBy(() -> jdbc.update(sql))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertThat(ex.getMessage().toLowerCase()).contains(constraint));
    }

    static Stream<Arguments> violations() {
        return Stream.of(
                Arguments.of("uq_users_email", userInsert("fixture@example.com", "CUSTOMER")),
                Arguments.of("ck_users_email_lower", userInsert("Upper@Example.com", "CUSTOMER")),
                Arguments.of("ck_users_role", userInsert("guest@example.com", "GUEST")),
                Arguments.of("ck_users_points", "UPDATE users SET gift_points_balance = -1 WHERE id = " + USER),
                Arguments.of("ck_addresses_zip", "INSERT INTO addresses (user_id, first_name, last_name, email, "
                        + "phone, street_address, city, province, zip_code, country, is_default, created_at) VALUES ("
                        + USER + ", 'A', 'B', 'a@example.com', '+639171234567', '1 Rizal St', 'Makati', "
                        + "'Metro Manila', '123', 'Philippines', TRUE, " + NOW + ")"),
                Arguments.of("ck_books_price", "UPDATE books SET price = -1 WHERE id = " + BOOK),
                Arguments.of("ck_books_stock", "UPDATE books SET stock_quantity = -1 WHERE id = " + BOOK),
                Arguments.of("ck_books_format", "UPDATE books SET format = 'AUDIOBOOK' WHERE id = " + BOOK),
                Arguments.of("uq_books_title_format", "INSERT INTO books (title, description, format, language, "
                        + "price, stock_quantity, copies_sold, publish_date, category_id, author_id, publisher_id, "
                        + "created_at, updated_at) SELECT title, description, format, language, price, stock_quantity, "
                        + "copies_sold, publish_date, category_id, author_id, publisher_id, created_at, updated_at "
                        + "FROM books WHERE id = " + BOOK),
                Arguments.of("ck_cart_items_qty", "UPDATE cart_items SET quantity = 0 WHERE user_id = " + USER),
                Arguments.of("uq_cart_items_user_book", "INSERT INTO cart_items (user_id, book_id, quantity, "
                        + "created_at, updated_at) VALUES (" + USER + ", " + BOOK + ", 2, " + NOW + ", " + NOW + ")"),
                Arguments.of("uq_orders_number", orderInsert("BW-TEST-1")),
                Arguments.of("ck_orders_status", "UPDATE orders SET status = 'LOST' WHERE id = " + ORDER),
                Arguments.of("ck_orders_total", "UPDATE orders SET total_amount = 999.00 WHERE id = " + ORDER),
                Arguments.of("ck_orders_amounts", "UPDATE orders SET delivery_charge = -1.00, total_amount = 111.00 "
                        + "WHERE id = " + ORDER),
                Arguments.of("ck_order_items_qty", orderItemInsert(0, "0.00")),
                Arguments.of("ck_order_items_line", orderItemInsert(2, "300.00")),
                Arguments.of("ck_payments_method", paymentInsert("CASH", "NULL", "TXN-2", "SUCCESS")),
                Arguments.of("ck_payments_status", paymentInsert("E_WALLET", "NULL", "TXN-2", "PENDING")),
                Arguments.of("ck_payments_last4", paymentInsert("DEBIT_CARD", "'42'", "TXN-2", "FAILED")),
                Arguments.of("ck_payments_card", paymentInsert("CREDIT_CARD", "NULL", "TXN-2", "FAILED")),
                Arguments.of("uq_payments_txn", paymentInsert("E_WALLET", "NULL", "TXN-1", "FAILED")),
                Arguments.of("ck_gpt_type", ledgerInsert("BONUS", 5)),
                Arguments.of("ck_gpt_sign", ledgerInsert("EARNED", -5)));
    }

    @Test
    void validRowsAreAccepted() {
        jdbc.update(paymentInsert("E_WALLET", "NULL", "TXN-2", "FAILED"));
        jdbc.update(ledgerInsert("EARNED", 1));
        jdbc.update(ledgerInsert("REDEEMED", -1));
        jdbc.update(orderItemInsert(2, "850.00"));
    }

    @Test
    void noColumnCanHoldFullCardOrWalletDetails() {
        Integer sensitive = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE lower(table_schema) = 'public' AND (lower(column_name) LIKE '%card_number%' "
                + "OR lower(column_name) LIKE '%cvv%' OR lower(column_name) LIKE '%expir%' "
                + "OR lower(column_name) LIKE '%wallet%')", Integer.class);
        assertThat(sensitive).isZero();
    }

    private static String userInsert(String email, String role) {
        return "INSERT INTO users (email, password_hash, first_name, last_name, role, gift_points_balance, "
                + "created_at, updated_at) VALUES ('" + email + "', 'hash', 'Fix', 'Ture', '" + role + "', 0, "
                + NOW + ", " + NOW + ")";
    }

    /** Subtotal 100 + VAT 12 + delivery 0 - points 0 = total 112, so the total CHECK holds. */
    private static String orderInsert(String number) {
        return "INSERT INTO orders (order_number, user_id, status, ship_first_name, ship_last_name, ship_email, "
                + "ship_phone, ship_street_address, ship_city, ship_province, ship_zip_code, ship_country, "
                + "subtotal, vat_rate, vat_amount, delivery_charge, gift_points_redeemed, gift_points_amount, "
                + "total_amount, gift_points_earned, estimated_delivery_date, placed_at, updated_at) VALUES ('"
                + number + "', " + USER + ", 'PENDING_PAYMENT', 'Fix', 'Ture', 'fixture@example.com', "
                + "'+639171234567', '1 Rizal St', 'Makati', 'Metro Manila', '1200', 'Philippines', "
                + "100.00, 0.1200, 12.00, 0.00, 0, 0.00, 112.00, 0, CURRENT_DATE, " + NOW + ", " + NOW + ")";
    }

    /** Unit price is 425.00 (Dracula), so a correct line total is 425.00 times the quantity. */
    private static String orderItemInsert(int quantity, String lineTotal) {
        return "INSERT INTO order_items (order_id, book_id, title, format, unit_price, quantity, line_total) "
                + "VALUES (" + ORDER + ", " + BOOK + ", 'Dracula', 'PAPERBACK', 425.00, " + quantity + ", "
                + lineTotal + ")";
    }

    private static String paymentInsert(String method, String last4, String txn, String status) {
        return "INSERT INTO payments (order_id, method, amount, status, card_last4, transaction_id, created_at) "
                + "VALUES (" + ORDER + ", '" + method + "', 112.00, '" + status + "', " + last4 + ", '" + txn
                + "', " + NOW + ")";
    }

    private static String ledgerInsert(String type, int points) {
        return "INSERT INTO gift_point_transactions (user_id, order_id, type, points, created_at) VALUES ("
                + USER + ", " + ORDER + ", '" + type + "', " + points + ", " + NOW + ")";
    }
}
