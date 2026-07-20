package com.lostark.tracker.web;

import com.lostark.tracker.market.MarketSearchService;
import com.lostark.tracker.market.dto.MarketSearchResponse;
import com.lostark.tracker.web.error.InvalidRequestException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Read API for 아바타·모험의 서 실시간 시세 검색 (거래소 카테고리 확장). Thin passthrough to
 * {@link MarketSearchService}; no persistence. Category codes and the avatar part set are pinned server-side
 * so a client cannot point the search at an arbitrary category.
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    /** 아바타 상위 — a CharacterClass filter against this returns all parts for that class. */
    private static final String AVATAR_ROOT = "20000";
    /** The 10 avatar part categories (전체 제외). A {@code part} outside this set is a 400 — never forwarded. */
    private static final Set<String> AVATAR_PARTS = Set.of(
            "20005", "20010", "20020", "20030", "20050", "20060", "20070", "21400", "21500", "21600");

    private final MarketSearchService service;

    public MarketController(MarketSearchService service) {
        this.service = service;
    }

    /** The 30 playable classes for the avatar 직업 드롭다운. */
    @GetMapping("/classes")
    public List<String> classes() {
        return service.getClasses();
    }

    /**
     * 모험의 서 전량 (~140). No paging, no sort, no query params.
     *
     * <p>Was a paged passthrough until 2026-07-20. The 대륙별 분류 view needs one 대륙's 7 collectibles,
     * which are scattered across all ~14 pages (the upstream category has no sub-categories and no
     * continent filter — 실측), so a page-at-a-time contract cannot serve it. The client holds the
     * 대륙 map and does 검색·정렬 locally over these rows; see {@link MarketSearchService#getAdventureAll()}
     * for why that is also cheaper in API calls than the old contract.
     */
    @GetMapping("/adventure")
    public MarketSearchResponse adventure() {
        return service.getAdventureAll();
    }

    /**
     * 아바타 검색 — {@code class} is REQUIRED (사용자 결정: without it a single part is ~2,100 items).
     * {@code part} optionally narrows to one of the 10 body parts; omitted, it searches all parts of the
     * class via the root category. {@code q}/sort/page as usual.
     */
    @GetMapping("/avatar")
    public MarketSearchResponse avatar(
            @RequestParam(name = "class") String characterClass,
            @RequestParam(required = false) String part,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "min_price") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "1") int page) {
        if (characterClass == null || characterClass.isBlank()) {
            throw new InvalidRequestException("직업을 선택해 주세요");
        }
        String category = AVATAR_ROOT;
        if (part != null && !part.isBlank()) {
            if (!AVATAR_PARTS.contains(part)) {
                throw new InvalidRequestException("부위 코드가 올바르지 않습니다");
            }
            category = part;
        }
        return service.search(category, characterClass, q, page, sort, dir);
    }
}
