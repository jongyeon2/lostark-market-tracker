import { ChevronLeft, ChevronRight } from 'lucide-react'

import { Button } from '@/components/ui/button'

/*
  페이지네이션 — 이전/다음 + 페이지 번호(인게임 거래소 "116/211" 방식). 업스트림 PageSize=10 고정이라
  총 페이지 = ceil(totalCount / 10). 번호는 현재 주변 일부만 노출(윈도우)해 211페이지여도 UI가 안 넘친다.
*/
const PAGE_SIZE = 10
const WINDOW = 2 // 현재 페이지 좌우로 보여줄 번호 수

export function Pagination({
  page,
  totalCount,
  onPageChange,
}: {
  page: number
  totalCount: number
  onPageChange: (page: number) => void
}) {
  const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE))
  if (totalPages <= 1) return null

  const from = Math.max(1, page - WINDOW)
  const to = Math.min(totalPages, page + WINDOW)
  const numbers: number[] = []
  for (let i = from; i <= to; i++) numbers.push(i)

  return (
    <nav className="flex items-center justify-center gap-1" aria-label="페이지 이동">
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page - 1)}
        disabled={page <= 1}
        aria-label="이전 페이지"
      >
        <ChevronLeft className="size-4" />
      </Button>

      {from > 1 && (
        <>
          <PageButton n={1} active={page === 1} onClick={onPageChange} />
          {from > 2 && <span className="text-muted-foreground px-1 text-sm">…</span>}
        </>
      )}

      {numbers.map((n) => (
        <PageButton key={n} n={n} active={n === page} onClick={onPageChange} />
      ))}

      {to < totalPages && (
        <>
          {to < totalPages - 1 && <span className="text-muted-foreground px-1 text-sm">…</span>}
          <PageButton n={totalPages} active={page === totalPages} onClick={onPageChange} />
        </>
      )}

      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages}
        aria-label="다음 페이지"
      >
        <ChevronRight className="size-4" />
      </Button>
    </nav>
  )
}

function PageButton({
  n,
  active,
  onClick,
}: {
  n: number
  active: boolean
  onClick: (page: number) => void
}) {
  return (
    <Button
      variant={active ? 'default' : 'outline'}
      size="sm"
      className="min-w-9 tabular-nums"
      aria-current={active ? 'page' : undefined}
      onClick={() => onClick(n)}
    >
      {n}
    </Button>
  )
}