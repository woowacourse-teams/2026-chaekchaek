package com.chaekchaek.book.client;

import java.net.URI;
import java.util.regex.Pattern;

public final class Yes24ImageUrlGenerator {

    private static final String YES24_IMAGE_HOST = "image.yes24.com";
    private static final Pattern GOODS_ID_PATTERN = Pattern.compile("\\d+");

    private Yes24ImageUrlGenerator() {

    }

    public static Yes24ImageUrls fromCoverUrl(String coverImageUrl) {
        if (coverImageUrl == null || coverImageUrl.isBlank()) {
            return Yes24ImageUrls.empty();
        }

        URI uri;
        try {
            uri = URI.create(coverImageUrl);
        } catch (IllegalArgumentException e) {
            return Yes24ImageUrls.empty();
        }

        if (!isYes24ImageUrl(uri)) {
            return Yes24ImageUrls.empty();
        }

        String[] pathSegments = uri.getPath().split("/");
        if (pathSegments.length != 4 || !"goods".equals(pathSegments[1])) {
            return Yes24ImageUrls.empty();
        }

        String goodsId = pathSegments[2];
        if (!GOODS_ID_PATTERN.matcher(goodsId).matches()) {
            return Yes24ImageUrls.empty();
        }

        String baseUrl = "https://image.yes24.com/goods/" + goodsId;

        return new Yes24ImageUrls(
                baseUrl + "/side",
                baseUrl + "/back"
        );
    }

    private static boolean isYes24ImageUrl(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme())
                && YES24_IMAGE_HOST.equalsIgnoreCase(uri.getHost());
    }
}
