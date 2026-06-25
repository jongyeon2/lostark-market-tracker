// Intentional empty route skeleton — the per-event 전후 변화율 table, ok/insufficient_data
// distinction, and the "상관 ≠ 인과" 고지 (IMPCT-01..04) land in Phase 10. No fetching yet.
export function ImpactPage() {
  return (
    <div className="space-y-4">
      <h1 className="text-[28px] leading-tight font-semibold">이벤트 영향</h1>
      <p className="text-muted-foreground text-base">
        이벤트별 전후 변화율 · ok/insufficient_data 구분 · "상관 ≠ 인과" 고지는 Phase 10에서
        채워집니다.
      </p>
    </div>
  )
}