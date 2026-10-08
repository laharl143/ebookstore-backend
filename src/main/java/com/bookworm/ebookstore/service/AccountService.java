package com.bookworm.ebookstore.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookworm.ebookstore.config.StoreProperties;
import com.bookworm.ebookstore.dto.AddressRequest;
import com.bookworm.ebookstore.dto.AddressResponse;
import com.bookworm.ebookstore.dto.BookSummaryResponse;
import com.bookworm.ebookstore.dto.UserResponse;
import com.bookworm.ebookstore.entity.Address;
import com.bookworm.ebookstore.entity.Book;
import com.bookworm.ebookstore.entity.BookFormat;
import com.bookworm.ebookstore.entity.Genre;
import com.bookworm.ebookstore.entity.OrderStatus;
import com.bookworm.ebookstore.entity.User;
import com.bookworm.ebookstore.exception.AuthenticationException;
import com.bookworm.ebookstore.mapper.AddressMapper;
import com.bookworm.ebookstore.mapper.BookMapper;
import com.bookworm.ebookstore.mapper.UserMapper;
import com.bookworm.ebookstore.repository.AddressRepository;
import com.bookworm.ebookstore.repository.BookRepository;
import com.bookworm.ebookstore.repository.UserRepository;

@Service
public class AccountService {

    private static final Set<OrderStatus> BOUGHT_STATUSES =
            Set.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final BookRepository bookRepository;
    private final StoreProperties storeProperties;
    private final Clock clock;

    public AccountService(
            UserRepository userRepository,
            AddressRepository addressRepository,
            BookRepository bookRepository,
            StoreProperties storeProperties,
            Clock clock) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.bookRepository = bookRepository;
        this.storeProperties = storeProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        return UserMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserIdWithCustomSort(userId).stream()
                .map(AddressMapper::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        boolean hasPriorAddresses = addressRepository.existsByUserId(userId);
        boolean isDefault;

        if (!hasPriorAddresses) {
            isDefault = true;
        } else {
            isDefault = Boolean.TRUE.equals(request.isDefault());
            if (isDefault) {
                addressRepository.clearDefaultAddressesForUser(userId);
            }
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        Address address = AddressMapper.toEntity(request, user, isDefault, now);
        Address saved = addressRepository.save(address);

        return AddressMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BookSummaryResponse> getRecommendations(Long userId, int size) {
        List<Book> boughtBooks = bookRepository.findBoughtBooks(userId, BOUGHT_STATUSES);

        Set<String> boughtTitlesLower = boughtBooks.stream()
                .map(b -> b.getTitle().toLowerCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> boughtAuthorIds = boughtBooks.stream()
                .map(b -> b.getAuthor().getId())
                .collect(Collectors.toSet());

        Set<Long> boughtGenreIds = boughtBooks.stream()
                .flatMap(b -> b.getGenres().stream())
                .map(Genre::getId)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now(clock.withZone(storeProperties.zone()));

        List<BookSummaryResponse> result = new ArrayList<>();

        if (!boughtBooks.isEmpty()) {
            // Candidate books: share author or genre with bought books, exclude bought titles
            List<Book> allBooks = bookRepository.findAll();
            List<Book> candidates = allBooks.stream()
                    .filter(b -> !boughtTitlesLower.contains(b.getTitle().toLowerCase()))
                    .filter(b -> boughtAuthorIds.contains(b.getAuthor().getId())
                            || b.getGenres().stream().anyMatch(g -> boughtGenreIds.contains(g.getId())))
                    .sorted(recommendationComparator(boughtAuthorIds, boughtGenreIds))
                    .limit(size)
                    .toList();

            for (Book b : candidates) {
                result.add(BookMapper.toBookSummaryResponse(b,
                        estimatedDeliveryDate(b.getFormat(), today), storeProperties.currency()));
            }
        }

        if (result.size() < size) {
            // Top up with newest books not already included and not bought
            Set<String> includedTitles = new LinkedHashSet<>(boughtTitlesLower);
            result.stream()
                    .map(r -> r.title().toLowerCase())
                    .forEach(includedTitles::add);

            int needed = size - result.size();
            List<Book> topUp = bookRepository.findNewestExcludingTitles(includedTitles).stream()
                    .limit(needed)
                    .toList();

            for (Book b : topUp) {
                result.add(BookMapper.toBookSummaryResponse(b,
                        estimatedDeliveryDate(b.getFormat(), today), storeProperties.currency()));
            }
        }

        return result;
    }

    private Comparator<Book> recommendationComparator(Set<Long> boughtAuthorIds, Set<Long> boughtGenreIds) {
        return Comparator
                .<Book, Integer>comparing(b -> score(b, boughtAuthorIds, boughtGenreIds), Comparator.reverseOrder())
                .thenComparing(Comparator.comparingInt(Book::getCopiesSold).reversed())
                .thenComparing(Book::getId);
    }

    private int score(Book b, Set<Long> boughtAuthorIds, Set<Long> boughtGenreIds) {
        int authorScore = boughtAuthorIds.contains(b.getAuthor().getId()) ? 2 : 0;
        long sharedGenres = b.getGenres().stream()
                .filter(g -> boughtGenreIds.contains(g.getId()))
                .count();
        return authorScore + (int) sharedGenres;
    }

    private LocalDate estimatedDeliveryDate(BookFormat format, LocalDate today) {
        if (format == BookFormat.EBOOK) {
            return today;
        }
        return today.plusDays(storeProperties.deliveryDays());
    }
}
