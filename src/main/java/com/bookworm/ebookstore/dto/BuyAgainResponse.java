package com.bookworm.ebookstore.dto;

import java.util.List;

public record BuyAgainResponse(
        CartResponse cart,
        List<AddedItem> added,
        List<SkippedItem> skipped
) {
}
