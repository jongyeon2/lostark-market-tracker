import { Skeleton } from '@/components/ui/skeleton'

// FND-04 / D-07: shared loading state — skeleton placeholders + the UI-SPEC helper text.
// Purely presentational (no data fetching); centered in the content area.
export function LoadingState() {
  return (
    <div className="flex flex-col items-center gap-4 py-16" role="status" aria-busy="true">
      <div className="w-full max-w-md space-y-3">
        <Skeleton className="h-8 w-1/2" />
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-20 w-full" />
      </div>
      <p className="text-muted-foreground text-sm">불러오는 중…</p>
    </div>
  )
}