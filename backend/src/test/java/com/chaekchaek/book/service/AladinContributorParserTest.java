package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AladinContributorParserTest {

    @Test
    @DisplayName("역할이 표시되어 있다면 지은이와 옮긴이를 구분한다")
    void should_SeparateAuthorsAndTranslators_When_RolesArePresent() {
        // given
        String contributors = "앤디 위어 (지은이), 박아람 (옮긴이)";

        // when
        AladinContributorParser.Contributors result = AladinContributorParser.parse(contributors);

        // then
        assertThat(result.authors()).containsExactly("앤디 위어");
        assertThat(result.translators()).containsExactly("박아람");
    }

    @Test
    @DisplayName("같은 역할의 인물이 여러 명이라면 각각 분리한다")
    void should_SplitContributors_When_MultipleNamesShareRole() {
        // given
        String contributors = "엘리에저 유드코스키, 네이트 소아레스 (지은이), 윤지관, 전승희 (옮긴이)";

        // when
        AladinContributorParser.Contributors result = AladinContributorParser.parse(contributors);

        // then
        assertThat(result.authors()).containsExactly("엘리에저 유드코스키", "네이트 소아레스");
        assertThat(result.translators()).containsExactly("윤지관", "전승희");
    }

    @Test
    @DisplayName("역할 표시가 없다면 모든 인물을 지은이로 간주한다")
    void should_TreatAllContributorsAsAuthors_When_RoleIsAbsent() {
        // given
        String contributors = "로버트 C. 마틴, 마이클 페더스";

        // when
        AladinContributorParser.Contributors result = AladinContributorParser.parse(contributors);

        // then
        assertThat(result.authors()).containsExactly("로버트 C. 마틴", "마이클 페더스");
        assertThat(result.translators()).isEmpty();
    }

    @Test
    @DisplayName("옮긴이가 없다면 빈 옮긴이 목록을 반환한다")
    void should_ReturnEmptyTranslators_When_BookHasOnlyAuthor() {
        // given
        String contributors = "한강 (지은이)";

        // when
        AladinContributorParser.Contributors result = AladinContributorParser.parse(contributors);

        // then
        assertThat(result.authors()).containsExactly("한강");
        assertThat(result.translators()).isEmpty();
    }

    @Test
    @DisplayName("비대상 역할만 표시되어 있다면 지은이와 옮긴이에서 제외한다")
    void should_IgnoreContributors_When_OnlyUnsupportedRoleIsPresent() {
        // given
        String contributors = "어린이철학교육연구소 (엮은이)";

        // when
        AladinContributorParser.Contributors result = AladinContributorParser.parse(contributors);

        // then
        assertThat(result.authors()).isEmpty();
        assertThat(result.translators()).isEmpty();
    }

    @Test
    @DisplayName("여러 역할이 섞여 있다면 지은이와 옮긴이만 추출한다")
    void should_ExtractOnlySupportedContributors_When_RolesAreMixed() {
        // given
        String contributors = "제인 오스틴 (지은이), 홍길동 (채색), 윤지관, 전승희 (옮긴이)";

        // when
        AladinContributorParser.Contributors result = AladinContributorParser.parse(contributors);

        // then
        assertThat(result.authors()).containsExactly("제인 오스틴");
        assertThat(result.translators()).containsExactly("윤지관", "전승희");
    }

    @Test
    @DisplayName("괄호 별칭만 있다면 역할 없는 지은이로 간주한다")
    void should_TreatParentheticalAliasAsAuthor_When_RoleIsAbsent() {
        // given
        String contributors = "지현이(디지털거북이), 홍길동";

        // when
        AladinContributorParser.Contributors result = AladinContributorParser.parse(contributors);

        // then
        assertThat(result.authors()).containsExactly("지현이(디지털거북이)", "홍길동");
        assertThat(result.translators()).isEmpty();
    }

    @Test
    @DisplayName("괄호 안의 쉼표가 있다면 인물을 분리하지 않는다")
    void should_PreserveContributor_When_CommaIsInsideParentheses() {
        // given
        String contributors = "민관식 (필명, 청원) (지은이)";

        // when
        AladinContributorParser.Contributors result = AladinContributorParser.parse(contributors);

        // then
        assertThat(result.authors()).containsExactly("민관식 (필명, 청원)");
        assertThat(result.translators()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("원문이 null이거나 비어 있다면 빈 기여자 목록을 반환한다")
    void should_ReturnEmptyContributors_When_SourceIsNullOrBlank(String source) {
        // when
        AladinContributorParser.Contributors result = AladinContributorParser.parse(source);

        // then
        assertThat(result.authors()).isEmpty();
        assertThat(result.translators()).isEmpty();
    }
}
