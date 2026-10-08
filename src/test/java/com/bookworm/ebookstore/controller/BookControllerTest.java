package com.bookworm.ebookstore.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.bookworm.ebookstore.config.ProblemAuthenticationEntryPoint;
import com.bookworm.ebookstore.config.SecurityBeansConfig;
import com.bookworm.ebookstore.config.SecurityConfig;
import com.bookworm.ebookstore.dto.AuthorResponse;
import com.bookworm.ebookstore.dto.AuthorSummary;
import com.bookworm.ebookstore.dto.BookDetailResponse;
import com.bookworm.ebookstore.dto.BookSummaryResponse;
import com.bookworm.ebookstore.dto.CategoryResponse;
import com.bookworm.ebookstore.dto.PageResponse;
import com.bookworm.ebookstore.dto.PublisherResponse;
import com.bookworm.ebookstore.dto.PublisherSummary;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.GlobalExceptionHandler;
import com.bookworm.ebookstore.service.BookService;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookController.class)
@Import({SecurityConfig.class, SecurityBeansConfig.class, ProblemAuthenticationEntryPoint.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    @DisplayName("GET /api/v1/categories - returns 200 and list of CategoryResponse")
    void getCategoriesReturnsList() throws Exception {
        CategoryResponse romance = new CategoryResponse(1L, "Romance", "romance");
        when(bookService.getCategories()).thenReturn(List.of(romance));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Romance")))
                .andExpect(jsonPath("$[0].slug", is("romance")));
    }

    @Test
    @DisplayName("GET /api/v1/books - returns 200 and PageResponse of BookSummaryResponse")
    void getBooksReturnsPage() throws Exception {
        BookSummaryResponse bookSummary = new BookSummaryResponse(
                1L, "Sample Book", "Short desc", "PAPERBACK", "English",
                BigDecimal.valueOf(199.00), "PHP", "front-url",
                new AuthorSummary(1L, "Author Name"),
                new PublisherSummary(1L, "Publisher Name"),
                List.of("Fiction"), true, LocalDate.now()
        );

        PageResponse<BookSummaryResponse> pageResponse = new PageResponse<>(
                List.of(bookSummary), 0, 12, 1L, 1
        );

        when(bookService.getBooks(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(pageResponse);

        // Simple default search request
        mockMvc.perform(get("/api/v1/books")
                        .param("q", "Sample")
                        .param("category", "romance")
                        .param("minPrice", "10.00")
                        .param("maxPrice", "500.00")
                        .param("page", "0")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Sample Book")))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/books - invalid format returns 400 MALFORMED_REQUEST")
    void getBooksInvalidFormatReturnsMalformedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("format", "AUDIO"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));
    }

    @Test
    @DisplayName("GET /api/v1/books - invalid sort returns 400 MALFORMED_REQUEST")
    void getBooksInvalidSortReturnsMalformedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("sort", "unsupported_sort"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));
    }

    @Test
    @DisplayName("GET /api/v1/books - search query q exceeding 100 characters returns 400 VALIDATION_FAILED")
    void getBooksTooLongSearchQueryReturnsValidationFailed() throws Exception {
        String longQ = "a".repeat(101);
        mockMvc.perform(get("/api/v1/books").param("q", longQ))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.errors.q").exists());
    }

    @Test
    @DisplayName("GET /api/v1/books - size exceeding 50 returns 400 VALIDATION_FAILED")
    void getBooksSizeExceedingMaximumReturnsValidationFailed() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.errors.size").exists());
    }

    @Test
    @DisplayName("GET /api/v1/books/{id} - returns 200 and detailed book response")
    void getBookByIdReturnsDetails() throws Exception {
        BookDetailResponse bookDetail = new BookDetailResponse(
                1L, "Sample Book", "Description", "PAPERBACK", "English",
                BigDecimal.valueOf(199.00), "PHP", "front-url", "back-url",
                new AuthorResponse(1L, "Author Name", "photo", "bio"),
                new PublisherResponse(1L, "Publisher Name", "desc"),
                new CategoryResponse(1L, "Category Name", "category"),
                List.of("Fiction"), 10, 5, LocalDate.now(), true, LocalDate.now(), List.of()
        );

        when(bookService.getBookById(1L)).thenReturn(bookDetail);

        mockMvc.perform(get("/api/v1/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Sample Book")))
                .andExpect(jsonPath("$.description", is("Description")))
                .andExpect(jsonPath("$.copiesSold", is(5)));
    }

    @Test
    @DisplayName("GET /api/v1/authors - returns paged list of authors")
    void getAuthorsReturnsPagedList() throws Exception {
        AuthorResponse author = new AuthorResponse(1L, "Author Name", "photo-url", "bio-text");
        PageResponse<AuthorResponse> pageResponse = new PageResponse<>(List.of(author), 0, 12, 1L, 1);

        when(bookService.getAuthors(0, 12)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Author Name")));
    }
}
