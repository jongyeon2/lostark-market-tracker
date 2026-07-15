package com.lostark.tracker.gem;

import java.util.List;

/**
 * Read contract for the 보석 현재가 page (GEM-02). Deliberately NOT the 거래소 shape: an auction
 * listing has no {@code Id} (Phase 24 §H5), so a gem is keyed by its 계열 + 레벨, and the price is a
 * 최저 즉시구매가 rather than 거래소's 최저 호가.
 */
public final class GemDtos {

    private GemDtos() {
    }

    /**
     * Why a gem row has (or has not) a price. Modeled explicitly so a missing price can never be
     * rendered as a number — the {@code insufficient_data} precedent from event-impact, where every
     * anchor/price field goes null rather than defaulting to 0.
     */
    public enum GemPriceStatus {
        /** 최저 즉시구매가를 구했다. {@code minBuyPrice} non-null. */
        OK,
        /** 매물은 있으나 즉시구매(BuyPrice)를 건 것이 하나도 없다 — 입찰 전용. {@code minBuyPrice} null. */
        NO_BUYOUT,
        /**
         * 레이트리밋 토큰을 못 얻어 조회를 아예 하지 않았다 — 수집(Core Value)이 예산을 쓰는 중이라
         * 보석이 양보한 것이다. {@code FETCH_FAILED}와 분리한 이유: 이건 고장이 아니라 **설계된 양보**이고,
         * 사용자에게도 "잠시 후 다시"가 맞는 안내다. {@code minBuyPrice} null.
         */
        RATE_LIMITED,
        /** 이 보석 조회가 실패했다(다른 보석은 영향 없음). {@code minBuyPrice} null. */
        FETCH_FAILED
    }

    /**
     * One gem row. {@code minBuyPrice} is non-null ONLY when {@code status == OK} — the two fields move
     * together so the frontend cannot read a null as 0.
     *
     * @param series      계열 — 겁화 / 작열. 역할 주석은 붙이지 않는다: 보석은 역할로 나눠 쓰지 않고
     *                    레벨로 사서 실링으로 원하는 스킬에 돌려 낀다(Phase 27 GEM-04, 24-SPIKE §정정)
     * @param level       보석 레벨 8·9·10. NOTE: this is NOT the auction response's {@code Level} field,
     *                    which is the 아이템 레벨 (1640 for every gem, Phase 24 §H-Level). The gem's own
     *                    level exists only inside its name.
     * @param displayName 화면 라벨 — 레벨만("8레벨"). 계열은 그룹 헤더가 말한다(UI-SPEC).
     * @param iconUrl     로아 CDN 실아이콘 (6종 전부 distinct — 각인서와 달리 라벨 병기 불요)
     */
    public record GemPrice(
            String series,
            int level,
            String displayName,
            String iconUrl,
            Long minBuyPrice,
            GemPriceStatus status
    ) {
    }

    /**
     * The whole page's payload. {@code updatedAt} is the UTC instant the snapshot was built — it is
     * surfaced so the user knows the price is a CACHED snapshot, not a live quote. Null only on a
     * fail-open read with no cache.
     */
    public record GemsResponse(List<GemPrice> gems, String updatedAt) {
    }
}
