import { useState } from 'react'

import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { ItemIcon } from '@/features/_shared/ItemIcon'
import { RoleBadge } from '@/features/_shared/RoleBadge'
import { ApiError } from '@/lib/api'
import { cn } from '@/lib/utils'
import { useAddItem, useAdminItems, useDeactivateItem, useReactivateItem } from '@/lib/queries'
import type { AdminItemRequest, TrackedItem } from '@/lib/schemas'

/*
  워치리스트 CRUD section (ADMINUI-04), replacing the 15-02 placeholder. Consumes 15-01's
  GET /api/admin/items (active+inactive, D-13) so the admin sees deactivated items the public
  active-only GET cannot show (D-12). An inline add form (D-07) POSTs create (201). Each row shows an
  활성(up/green) / 비활성(neutral/slate) badge (StatusBadge precedent) with a destructive 비활성 gated by
  an inline 2-step confirm (D-09, soft-delete 204) or an accent 재활성 that re-POSTs the externalItemId
  (200, D-13). Every mutation invalidates ['admin-items'] → refetch (D-10) with inline feedback; a 409
  on add shows the specific duplicate warning; a 401 triggers the 15-02 global auto-logout (D-03).
*/
const MUTATION_FAIL = '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.'
const DUPLICATE_WARNING = '이미 활성 상태인 품목이에요.'
const CONNECTION_ERROR =
  '백엔드에 연결하지 못했어요. admin 백엔드(:8080)가 켜져 있는지 확인하고 다시 시도하세요.'

type Feedback = { kind: 'success' | 'warning' | 'error'; text: string }

function ActiveBadge({ active }: { active: boolean }) {
  // Follows the StatusBadge/ImpactStatusBadge color-badge precedent (semantic bg/10 + text, not accent).
  return active ? (
    <Badge variant="secondary" className="bg-up/10 text-up text-sm font-semibold">
      활성
    </Badge>
  ) : (
    <Badge variant="secondary" className="bg-neutral/10 text-neutral text-sm font-semibold">
      비활성
    </Badge>
  )
}

export function WatchlistSection() {
  const itemsQuery = useAdminItems()
  const addItem = useAddItem()
  const deactivateItem = useDeactivateItem()
  const reactivateItem = useReactivateItem()

  const [externalItemId, setExternalItemId] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [category, setCategory] = useState('')
  const [confirmingDeactivateId, setConfirmingDeactivateId] = useState<number | null>(null)
  const [feedback, setFeedback] = useState<Feedback | null>(null)

  function submitAdd() {
    const body: AdminItemRequest = {
      externalItemId: externalItemId.trim(),
      displayName: displayName.trim(),
      category: category.trim() ? category.trim() : undefined,
    }
    addItem.mutate(body, {
      onSuccess: () => {
        setFeedback({ kind: 'success', text: '품목을 추가했어요.' })
        setExternalItemId('')
        setDisplayName('')
        setCategory('')
      },
      onError: (error) => {
        // A 409 means the item already exists and is active — a specific, actionable warning (D-13/ADMINUI-04).
        if (error instanceof ApiError && error.status === 409) {
          setFeedback({ kind: 'warning', text: DUPLICATE_WARNING })
        } else {
          setFeedback({ kind: 'error', text: MUTATION_FAIL })
        }
      },
    })
  }

  function confirmDeactivate(id: number) {
    deactivateItem.mutate(id, {
      onSuccess: () => {
        setFeedback({ kind: 'success', text: '품목을 비활성했어요.' })
        setConfirmingDeactivateId(null)
      },
      onError: () => setFeedback({ kind: 'error', text: MUTATION_FAIL }),
    })
  }

  function reactivate(item: TrackedItem) {
    reactivateItem.mutate(item, {
      onSuccess: () => setFeedback({ kind: 'success', text: '품목을 재활성했어요.' }),
      onError: () => setFeedback({ kind: 'error', text: MUTATION_FAIL }),
    })
  }

  // 15-01 returns a deterministic order (active first, displayName asc) — render as-is.
  const items = itemsQuery.data ?? []
  const feedbackClass =
    feedback?.kind === 'error'
      ? 'text-destructive'
      : feedback?.kind === 'warning'
        ? 'text-warning'
        : undefined

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl">워치리스트</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Inline add form (D-07). */}
        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault()
            if (addItem.isPending) return
            submitAdd()
          }}
        >
          <div className="space-y-2">
            <label className="text-sm font-semibold" htmlFor="item-external-id">
              아이템 번호
            </label>
            <Input
              id="item-external-id"
              value={externalItemId}
              onChange={(event) => setExternalItemId(event.target.value)}
              placeholder="로스트아크 아이템 번호"
              required
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold" htmlFor="item-display-name">
              표시 이름
            </label>
            <Input
              id="item-display-name"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              required
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold" htmlFor="item-category">
              분류 (선택)
            </label>
            <Input
              id="item-category"
              value={category}
              onChange={(event) => setCategory(event.target.value)}
            />
          </div>

          <Button
            type="submit"
            disabled={addItem.isPending || externalItemId.trim() === '' || displayName.trim() === ''}
          >
            {addItem.isPending ? '추가 중…' : '품목 추가'}
          </Button>
        </form>

        {feedback && <p className={cn('text-base', feedbackClass)}>{feedback.text}</p>}

        {/* Active + inactive item list (from GET /api/admin/items, D-12/13). */}
        <AsyncBoundary
          status={itemsQuery.status}
          isEmpty={items.length === 0}
          onRetry={() => itemsQuery.refetch()}
          emptyHeading="워치리스트가 비어 있어요"
          emptyBody="위에서 로스트아크 아이템 번호로 품목을 추가하면 수집 대상이 됩니다."
          errorMessage={CONNECTION_ERROR}
        >
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>아이템</TableHead>
                <TableHead>번호</TableHead>
                <TableHead>상태</TableHead>
                <TableHead className="text-right">관리</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((item) => {
                const reactivating = reactivateItem.isPending && reactivateItem.variables?.id === item.id
                return (
                  <TableRow key={item.id}>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <ItemIcon iconUrl={item.iconUrl} roleGroup={item.roleGroup} size="sm" />
                        <span className="font-semibold whitespace-normal">{item.displayName}</span>
                        <RoleBadge roleGroup={item.roleGroup} />
                      </div>
                    </TableCell>
                    <TableCell className="tabular-nums">{item.externalItemId}</TableCell>
                    <TableCell>
                      <ActiveBadge active={item.active} />
                    </TableCell>
                    <TableCell className="text-right">
                      {item.active ? (
                        confirmingDeactivateId === item.id ? (
                          // Inline 2-step deactivate confirm (D-09).
                          <div className="flex flex-wrap items-center justify-end gap-2">
                            <span className="text-muted-foreground text-sm">
                              이 품목을 비활성할까요? 수집 대상에서 제외됩니다.
                            </span>
                            <Button
                              size="sm"
                              variant="destructive"
                              onClick={() => confirmDeactivate(item.id)}
                              disabled={deactivateItem.isPending}
                            >
                              비활성
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => setConfirmingDeactivateId(null)}
                              disabled={deactivateItem.isPending}
                            >
                              취소
                            </Button>
                          </div>
                        ) : (
                          <Button
                            size="sm"
                            variant="destructive"
                            onClick={() => {
                              setConfirmingDeactivateId(item.id)
                              setFeedback(null)
                            }}
                          >
                            비활성
                          </Button>
                        )
                      ) : (
                        // Reactivate = re-POST externalItemId (accent/primary, NOT destructive, D-13).
                        <Button size="sm" onClick={() => reactivate(item)} disabled={reactivating}>
                          {reactivating ? '재활성 중…' : '재활성'}
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </AsyncBoundary>
      </CardContent>
    </Card>
  )
}
