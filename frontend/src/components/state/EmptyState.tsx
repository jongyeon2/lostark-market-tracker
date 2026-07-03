// FND-04 / D-07: shared empty state — the UI-SPEC heading + seed-backend body copy.
// Both lines are overridable per screen (defaults preserve the original public-screen copy);
// the admin console (Phase 15) passes section-specific heading/body (e.g. '등록된 이벤트가 없어요').
const DEFAULT_HEADING = '표시할 데이터가 아직 없어요'
const DEFAULT_BODY = 'seed 프로파일 백엔드를 기동하면 시세가 채워집니다 (SPRING_PROFILES_ACTIVE=seed).'

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