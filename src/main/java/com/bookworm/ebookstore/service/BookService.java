package com.bookworm.ebookstore.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookworm.ebookstore.config.StoreProperties;
import com.bookworm.ebookstore.dto.AuthorResponse;
import com.bookworm.ebookstore.dto.BookDetailResponse;
import com.bookworm.ebookstore.dto.BookSummaryResponse;
import com.bookworm.ebookstore.dto.CategoryResponse;
import com.bookworm.ebookstore.dto.OtherFormat;
import com.bookworm.ebookstore.dto.PageResponse;
import com.bookworm.ebookstore.dto.PublisherResponse;
import com.bookworm.ebookstore.entity.Author;
import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.entity.Category;
import com.bookworm.ebookstore.entity.Publisher;
import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.bookworm.ebookstore.exception.ResourceNotFoundException;
import com.bookworm.ebookstore.exception.ValidationException;
import com.bookworm.ebookstore.mapper.BookMapper;
import com.bookworm.ebookstore.repository.AuthorRepository;
import com.bookworm.ebookstore.repository.BookRepository;
import com.bookworm.ebookstore.repository.CategoryRepository;
import com.bookworm.ebookstore.repository.PublisherRepository;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final StoreProperties storeProperties;
    private final Clock clock;

    public BookService(
            BookRepository bookRepository,
            CategoryRepository categoryRepository,
            AuthorRepository authorRepository,
            PublisherRepository publisherRepository,
            StoreProperties storeProperties,
            Clock clock) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.storeProperties = storeProperties;
        this.clock = clock;
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll(Sort.by("id")).stream()
                .map(BookMapper::toCategoryResponse)
                .toList();
    }

    public PageResponse<BookSummaryResponse> getBooks(
            String q,
            String categorySlug,
            Long authorId,
            Long publisherId,
            String language,
            String format,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sort,
            int page,
            int size) {

        // Range check validation
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("minPrice", "Minimum price cannot be greater than maximum price");
            throw new ValidationException(ApiErrorCode.VALIDATION_FAILED, "Price range validation failed", errors);
        }

        Specification<Book> spec = Specification.where(null);

        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = categoryRepository.findBySlug(categorySlug)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.CATEGORY_NOT_FOUND, "Category not found for slug: " + categorySlug));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }

        if (authorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("author").get("id"), authorId));
        }

        if (publisherId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("publisher").get("id"), publisherId));
        }

        if (language != null && !language.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("language")), language.toLowerCase()));
        }

        if (format != null && !format.isBlank()) {
            try {
                BookFormat f = BookFormat.valueOf(format.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("format"), f));
            } catch (IllegalArgumentException e) {
                // If invalid format is passed, we let it be handled by request validation/deserialization.
                // But just in case, we can filter by matching nothing if format doesn't exist.
                spec = spec.and((root, query, cb) -> cb.disjunction());
            }
        }

        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> cb.ge(root.get("price"), minPrice));
        }

        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.le(root.get("price"), maxPrice));
        }

        if (q != null && !q.isBlank()) {
            String escapedPattern = "%" + escapeLike(q).toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), escapedPattern, '\\'),
                    cb.like(cb.lower(root.get("author").get("name")), escapedPattern, '\\'),
                    cb.like(cb.lower(root.get("publisher").get("name")), escapedPattern, '\\')
            ));
        }

        PageRequest pageRequest = PageRequest.of(page, size, getSort(sort));
        Page<Book> bookPage = bookRepository.findAll(spec, pageRequest);

        LocalDate today = LocalDate.now(clock.withZone(storeProperties.zone()));
        List<BookSummaryResponse> content = bookPage.getContent().stream()
                .map(book -> BookMapper.toBookSummaryResponse(book, getEstimatedDeliveryDate(book.getFormat(), today), storeProperties.currency()))
                .toList();

        return new PageResponse<>(
                content,
                bookPage.getNumber(),
                bookPage.getSize(),
                bookPage.getTotalElements(),
                bookPage.getTotalPages()
        );
    }

    public BookDetailResponse getBookById(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.BOOK_NOT_FOUND, "Book not found with ID: " + bookId));

        LocalDate today = LocalDate.now(clock.withZone(storeProperties.zone()));
        LocalDate deliveryDate = getEstimatedDeliveryDate(book.getFormat(), today);

        List<OtherFormat> otherFormats = bookRepository.findByTitleAndIdNot(book.getTitle(), book.getId()).stream()
                .sorted(Comparator.comparing(Book::getFormat))
                .map(BookMapper::toOtherFormat)
                .toList();

        return BookMapper.toBookDetailResponse(book, deliveryDate, storeProperties.currency(), otherFormats);
    }

    public List<BookSummaryResponse> getRelatedBooks(Long bookId, Integer size) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.BOOK_NOT_FOUND, "Book not found with ID: " + bookId));

        int limitSize = size != null ? size : 8;

        List<Book> candidates = bookRepository.findAll().stream()
                .filter(b -> !b.getTitle().equalsIgnoreCase(book.getTitle()))
                .filter(b -> b.getAuthor().getId().equals(book.getAuthor().getId()) ||
                             b.getGenres().stream().anyMatch(g -> book.getGenres().contains(g)))
                .sorted(getRelatedComparator(book))
                .limit(limitSize)
                .toList();

        LocalDate today = LocalDate.now(clock.withZone(storeProperties.zone()));

        return candidates.stream()
                .map(b -> BookMapper.toBookSummaryResponse(b, getEstimatedDeliveryDate(b.getFormat(), today), storeProperties.currency()))
                .toList();
    }

    public PageResponse<AuthorResponse> getAuthors(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));
        Page<Author> authorPage = authorRepository.findAll(pageRequest);

        List<AuthorResponse> content = authorPage.getContent().stream()
                .map(BookMapper::toAuthorResponse)
                .toList();

        return new PageResponse<>(
                content,
                authorPage.getNumber(),
                authorPage.getSize(),
                authorPage.getTotalElements(),
                authorPage.getTotalPages()
        );
    }

    public AuthorResponse getAuthorById(Long authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.AUTHOR_NOT_FOUND, "Author not found with ID: " + authorId));
        return BookMapper.toAuthorResponse(author);
    }

    public PageResponse<PublisherResponse> getPublishers(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));
        Page<Publisher> publisherPage = publisherRepository.findAll(pageRequest);

        List<PublisherResponse> content = publisherPage.getContent().stream()
                .map(BookMapper::toPublisherResponse)
                .toList();

        return new PageResponse<>(
                content,
                publisherPage.getNumber(),
                publisherPage.getSize(),
                publisherPage.getTotalElements(),
                publisherPage.getTotalPages()
        );
    }

    public PublisherResponse getPublisherById(Long publisherId) {
        Publisher publisher = publisherRepository.findById(publisherId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PUBLISHER_NOT_FOUND, "Publisher not found with ID: " + publisherId));
        return BookMapper.toPublisherResponse(publisher);
    }

    private LocalDate getEstimatedDeliveryDate(BookFormat format, LocalDate today) {
        if (format == BookFormat.EBOOK) {
            return today;
        }
        return today.plusDays(storeProperties.deliveryDays());
    }

    private Sort getSort(String sort) {
        if (sort == null) {
            sort = "relevance";
        }
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            case "newest" -> Sort.by(Sort.Order.desc("publishDate"), Sort.Order.asc("id"));
            case "bestselling" -> Sort.by(Sort.Order.desc("copiesSold"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.asc("title").ignoreCase(), Sort.Order.asc("id")); // "relevance"
        };
    }

    private Comparator<Book> getRelatedComparator(Book book) {
        return (b1, b2) -> {
            boolean b1SameAuthor = b1.getAuthor().getId().equals(book.getAuthor().getId());
            boolean b2SameAuthor = b2.getAuthor().getId().equals(book.getAuthor().getId());
            if (b1SameAuthor != b2SameAuthor) {
                return b1SameAuthor ? -1 : 1;
            }

            long b1Shared = b1.getGenres().stream().filter(book.getGenres()::contains).count();
            long b2Shared = b2.getGenres().stream().filter(book.getGenres()::contains).count();
            if (b1Shared != b2Shared) {
                return Long.compare(b2Shared, b1Shared); // Descending
            }

            if (b1.getCopiesSold() != b2.getCopiesSold()) {
                return Integer.compare(b2.getCopiesSold(), b1.getCopiesSold()); // Descending
            }

            return b1.getId().compareTo(b2.getId());
        };
    }

    private String escapeLike(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
