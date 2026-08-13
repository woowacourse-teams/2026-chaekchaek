package com.chaekchaek.review.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;

/** A class is needed here to preserve the difference between an omitted and a null field. */
@Getter
public class ReviewUpdateRequest {

    private boolean contentPresent;
    private String content;
    private boolean quotePresent;
    private String quote;
    private boolean chapterPresent;
    private String chapter;
    private boolean currentPagePresent;
    private Integer currentPage;
    private boolean totalPagesPresent;
    private Integer totalPages;
    private boolean spoilerPresent;
    private Boolean isSpoiler;

    @JsonSetter("content")
    public void setContent(String content) { this.contentPresent = true; this.content = content; }

    @JsonSetter("quote")
    public void setQuote(String quote) { this.quotePresent = true; this.quote = quote; }

    @JsonSetter("chapter")
    public void setChapter(String chapter) { this.chapterPresent = true; this.chapter = chapter; }

    @JsonSetter("currentPage")
    public void setCurrentPage(Integer currentPage) { this.currentPagePresent = true; this.currentPage = currentPage; }

    @JsonSetter("totalPages")
    public void setTotalPages(Integer totalPages) { this.totalPagesPresent = true; this.totalPages = totalPages; }

    @JsonSetter("isSpoiler")
    public void setSpoiler(Boolean spoiler) { this.spoilerPresent = true; this.isSpoiler = spoiler; }

    public boolean hasUpdate() {
        return contentPresent || quotePresent || chapterPresent || currentPagePresent || spoilerPresent;
    }
}
