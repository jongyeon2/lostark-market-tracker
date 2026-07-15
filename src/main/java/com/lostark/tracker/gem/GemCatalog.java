package com.lostark.tracker.gem;

import java.util.List;

/**
 * The 티어4 보석 curation, locked by the Phase 24 spike (`24-SPIKE-FINDINGS.md`) and 비준 by the user
 * on 2026-07-15: 겁화 + 작열 × 레벨 8·9·10 = 6종.
 *
 * <p><b>The series carry NO role annotation, on purpose</b> (Phase 27, GEM-04). These constants were
 * once named {@code SERIES_DEALER}/{@code SERIES_SUPPORT} and the screen read "겁화 (딜러)" — that was a
 * guess, never a measurement. 보석 is not used per role: players buy by LEVEL and re-seat the gem onto
 * whichever skill they want with 실링 (사용자 정정 2026-07-15). The names below claim nothing beyond
 * what the API actually returns, so they cannot be wrong again. 겁화/작열 stay SEPARATE — they are
 * distinct items at distinct prices; only the role gloss was false.
 *
 * <p><b>Why a constant table and not the DB:</b> a gem is not a {@code tracked_item}. The auction
 * response carries no {@code Id} (Phase 24 §H5), nothing is collected on a tick, and no time series is
 * recorded — so there is no row to seed and no migration to write. {@code searchName} IS the key.
 *
 * <p><b>Why the level lives in the name:</b> the auction response's {@code Level} field is the 아이템
 * 레벨 (1640 for every gem); the gem's own level appears only inside its name, and the API offers no
 * numeric level filter. Exact-name search is therefore the only way to isolate one gem — measured, not
 * assumed (Phase 24).
 *
 * <p>Icons are the real 로아 CDN assets and are all DISTINCT (unlike the 18 각인서, which share
 * {@code use_9_25} and needed label disambiguation — 12-SPIKE D-06).
 */
final class GemCatalog {

    private GemCatalog() {
    }

    private static final String ICON_BASE = "https://cdn-lostark.game.onstove.com/efui_iconatlas/use/";

    static final String SERIES_GEOPHWA = "겁화";
    static final String SERIES_JAKYEOL = "작열";

    /**
     * One curated gem. {@code searchName} is sent verbatim as the auction {@code ItemName} filter —
     * it must stay exactly as the live API spells it ("8레벨 겁화의 보석").
     */
    record Entry(String series, int level, String searchName, String iconUrl) {
        String displayName() {
            return level + "레벨";
        }
    }

    /** 6종, findings 표 그대로. 순서 = 화면 표시 순서(계열별 · 레벨 오름차순). */
    static final List<Entry> ENTRIES = List.of(
            new Entry(SERIES_GEOPHWA, 8, "8레벨 겁화의 보석", ICON_BASE + "use_12_103.png"),
            new Entry(SERIES_GEOPHWA, 9, "9레벨 겁화의 보석", ICON_BASE + "use_12_104.png"),
            new Entry(SERIES_GEOPHWA, 10, "10레벨 겁화의 보석", ICON_BASE + "use_12_105.png"),
            new Entry(SERIES_JAKYEOL, 8, "8레벨 작열의 보석", ICON_BASE + "use_12_113.png"),
            new Entry(SERIES_JAKYEOL, 9, "9레벨 작열의 보석", ICON_BASE + "use_12_114.png"),
            new Entry(SERIES_JAKYEOL, 10, "10레벨 작열의 보석", ICON_BASE + "use_12_115.png"));
}
