// FND-04 / D-07 / 17-01 D-05: shared empty state — the UI-SPEC heading + a real-data-neutral body.
// The default body no longer assumes a seed profile boot (Phase 17 real-data transition): it reads as
// "the pipeline is still collecting or there is nothing to show yet", never leaking a profile/env name.
// Both lines are overridable per screen (defaults preserve the neutral public-screen copy); the admin
// console (Phase 15) and the collection-aware screens (17-01) pass section-specific heading/body.
const DEFAULT_HEADING = '표시할 데이터가 아직 없어요'
const DEFAULT_BODY = '수집기가 시세를 모으는 중이거나 아직 표시할 데이터가 없습니다. 잠시 후 다시 확인해 주세요.'

export function EmptyState({
  heading = DEFAULT_HEADING,
  body = DEFAULT_BODY,
}: {
  heading?: string
  body?: string
}) {
  return (
    <div className="flex flex-col items-center gap-4 py-16 text-center">
      <h2 className="text-xl font-semibold">{heading}</h2>
      <p className="text-muted-foreground max-w-md text-base">{body}</p>
    </div>
  )
}