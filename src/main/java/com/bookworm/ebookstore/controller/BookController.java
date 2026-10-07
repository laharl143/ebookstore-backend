package com.bookworm.ebookstore.controller;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookworm.ebookstore.dto.AuthorResponse;
import com.bookworm.ebookstore.dto.BookDetailResponse;
import com.bookworm.ebookstore.dto.BookSummaryResponse;
import com.bookworm.ebookstore.dto.CategoryResponse;
import com.bookworm.ebookstore.dto.PageResponse;
import com.bookworm.ebookstore.dto.PublisherResponse;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.BadRequestException;
import com.bookworm.ebookstore.service.BookService;

@RestController
@RequestMapping("/api/v1")
@Validated
public class BookController {

    private static final List<String> ALLOWED_SORTS = List.of("relevance", "price_asc", "price_desc", "newest", "bestselling");

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(bookService.getCategories());
    }

    @GetMapping("/books")
    public ResponseEntity<PageResponse<BookSummaryResponse>> getBooks(
            @RequestParam(required = false) @Size(max = 100, message = "Search query cannot exceed 100 characters") String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) @DecimalMin(value = "0.00", message = "minPrice must be greater than or equal to 0.00") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin(value = "0.00", message = "maxPrice must be greater than or equal to 0.00") BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be greater than or equal to 0") int page,
            @RequestParam(defaultValue = "12") @Min(value = 1, message = "size must be at least 1") @Max(value = 50, message = "size cannot exceed 50") int size
    ) {
        // Parameter validation
        if (format != null && !format.isBlank()) {
            try {
                BookFormat.valueOf(format.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(ApiErrorCode.MALFORMED_REQUEST, "Invalid format value: '" + format + "'");
            }
        }

        if (sort != null && !sort.isBlank() && !ALLOWED_SORTS.contains(sort.toLowerCase())) {
            throw new BadRequestException(ApiErrorCode.MALFORMED_REQUEST, "Invalid sort value: '" + sort + "'");
        }

        PageResponse<BookSummaryResponse> response = bookService.getBooks(
                q, category, authorId, publisherId, language, format, minPrice, maxPrice, sort, page, size
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/books/{bookId}")
    public ResponseEntity<BookDetailResponse> getBookById(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookService.getBookById(bookId));
    }

    @GetMapping("/books/{bookId}/related")
    public ResponseEntity<List<BookSummaryResponse>> getRelatedBooks(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "8") @Min(value = 1, message = "size must be at least 1") @Max(value = 20, message = "size cannot exceed 20") int size
    ) {
        return ResponseEntity.ok(bookService.getRelatedBooks(bookId, size));
    }

    @GetMapping("/authors")
    public ResponseEntity<PageResponse<AuthorResponse>> getAuthors(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be greater than or equal to 0") int page,
            @RequestParam(defaultValue = "12") @Min(value = 1, message = "size must be at least 1") @Max(value = 50, message = "size cannot exceed 50") int size
    ) {
        return ResponseEntity.ok(bookService.getAuthors(page, size));
    }

    @GetMapping("/authors/{authorId}")
    public ResponseEntity<AuthorResponse> getAuthorById(@PathVariable Long authorId) {
        return ResponseEntity.ok(bookService.getAuthorById(authorId));
    }

    @GetMapping("/publishers")
    public ResponseEntity<PageResponse<PublisherResponse>> getPublishers(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be greater than or equal to 0") int page,
            @RequestParam(defaultValue = "12") @Min(value = 1, message = "size must be at least 1") @Max(value = 50, message = "size cannot exceed 50") int size
    ) {
        return ResponseEntity.ok(bookService.getPublishers(page, size));
    }

    @GetMapping("/publishers/{publisherId}")
    public ResponseEntity<PublisherResponse> getPublisherById(@PathVariable Long publisherId) {
        return ResponseEntity.ok(bookService.getPublisherById(publisherId));
    }
}
