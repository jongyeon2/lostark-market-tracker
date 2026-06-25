// FND-04 / D-07: shared empty state — the UI-SPEC heading + seed-backend body copy.
// The body line is overridable per screen (one line only); heading is fixed.
const DEFAULT_BODY = 'seed 프로파일 백엔드를 기동하면 시세가 채워집니다 (SPRING_PROFILES_ACTIVE=seed).'

export function EmptyState({ body = DEFAULT_BODY }: { body?: string }) {
  return (
    <div className="flex flex-col items-center gap-4 py-16 text-center">
      <h2 className="text-xl font-semibold">표시할 데이터가 아직 없어요</h2>
      <p className="text-muted-foreground max-w-md text-base">{body}</p>
    </div>
  )
}