package com.bookworm.ebookstore.mapper;

import java.time.LocalDate;
import java.util.List;

import com.bookworm.ebookstore.dto.AuthorResponse;
import com.bookworm.ebookstore.dto.AuthorSummary;
import com.bookworm.ebookstore.dto.BookDetailResponse;
import com.bookworm.ebookstore.dto.BookSummaryResponse;
import com.bookworm.ebookstore.dto.CategoryResponse;
import com.bookworm.ebookstore.dto.OtherFormat;
import com.bookworm.ebookstore.dto.PublisherResponse;
import com.bookworm.ebookstore.dto.PublisherSummary;
import com.bookworm.ebookstore.entity.Author;
import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.entity.Category;
import com.bookworm.ebookstore.entity.Genre;
import com.bookworm.ebookstore.entity.Publisher;

public final class BookMapper {

    private BookMapper() {
    }

    public static CategoryResponse toCategoryResponse(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug()
        );
    }

    public static AuthorSummary toAuthorSummary(Author author) {
        if (author == null) {
            return null;
        }
        return new AuthorSummary(
                author.getId(),
                author.getName()
        );
    }

    public static AuthorResponse toAuthorResponse(Author author) {
        if (author == null) {
            return null;
        }
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getPhotoUrl(),
                author.getBio()
        );
    }

    public static PublisherSummary toPublisherSummary(Publisher publisher) {
        if (publisher == null) {
            return null;
        }
        return new PublisherSummary(
                publisher.getId(),
                publisher.getName()
        );
    }

    public static PublisherResponse toPublisherResponse(Publisher publisher) {
        if (publisher == null) {
            return null;
        }
        return new PublisherResponse(
                publisher.getId(),
                publisher.getName(),
                publisher.getDescription()
        );
    }

    public static OtherFormat toOtherFormat(Book book) {
        if (book == null) {
            return null;
        }
        return new OtherFormat(
                book.getId(),
                book.getFormat().name(),
                book.getPrice()
        );
    }

    public static BookSummaryResponse toBookSummaryResponse(Book book, LocalDate estimatedDeliveryDate, String currency) {
        if (book == null) {
            return null;
        }
        return new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                getShortDescription(book.getDescription()),
                book.getFormat().name(),
                book.getLanguage(),
                book.getPrice(),
                currency,
                book.getFrontCoverUrl(),
                toAuthorSummary(book.getAuthor()),
                toPublisherSummary(book.getPublisher()),
                book.getGenres().stream()
                        .map(Genre::getName)
                        .sorted()
                        .toList(),
                book.isInStock(),
                estimatedDeliveryDate
        );
    }

    public static BookDetailResponse toBookDetailResponse(Book book, LocalDate estimatedDeliveryDate, String currency, List<OtherFormat> otherFormats) {
        if (book == null) {
            return null;
        }
        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                book.getFormat().name(),
                book.getLanguage(),
                book.getPrice(),
                currency,
                book.getFrontCoverUrl(),
                book.getBackCoverUrl(),
                toAuthorResponse(book.getAuthor()),
                toPublisherResponse(book.getPublisher()),
                toCategoryResponse(book.getCategory()),
                book.getGenres().stream()
                        .map(Genre::getName)
                        .sorted()
                        .toList(),
                book.getFormat() == BookFormat.EBOOK ? null : book.getStockQuantity(),
                book.getCopiesSold(),
                book.getPublishDate(),
                book.isInStock(),
                estimatedDeliveryDate,
                otherFormats
        );
    }

    private static String getShortDescription(String description) {
        if (description == null) {
            return "";
        }
        if (description.length() <= 150) {
            return description;
        }
        String substring = description.substring(0, 150);
        int lastSpace = substring.lastIndexOf(' ');
        if (lastSpace != -1) {
            return substring.substring(0, lastSpace) + "...";
        }
        return substring + "...";
    }
}
