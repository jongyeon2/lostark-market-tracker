import { useState } from 'react'

import { AsyncBoundary } from '@/components/state/AsyncBoundary'
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
import { useAdminCoupons, useCreateCoupon, useDeleteCoupon, useReplaceCoupon } from '@/lib/queries'
import type { AdminCouponRequest, Coupon } from '@/lib/schemas'

/*
  쿠폰 CRUD section (COUPON-01), mirroring EventSection. An inline form (no dialog) at the top creates
  (POST 201) / edits (PUT full-replace, 200); the list below (expires_at asc — soonest expiry first)
  offers 수정 and a destructive 삭제 guarded by an inline 2-step confirm. UNLIKE EventSection, 만료일 is
  a date-only <Input type="date"> whose "YYYY-MM-DD" value is sent verbatim as expiresAt — NO KST↔UTC
  conversion (D-01, kstLocalToUtcIso deliberately unused). Every mutation invalidates ['admin-coupons']
  AND ['coupons'] → the dashboard panel refreshes too; a 401 triggers the global auto-logout (D-03).
*/

const MUTATION_FAIL = '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.'
const CONNECTION_ERROR =
  '백엔드에 연결하지 못했어요. admin 백엔드(:8080)가 켜져 있는지 확인하고 다시 시도하세요.'

type Feedback = { kind: 'success' | 'error'; text: string }

// Date-only "YYYY-MM-DD" (D-01) → "YYYY.MM.DD" for display. NOT formatKst (no UTC instant here).
function formatExpiry(expiresAt: string): string {
  return expiresAt.slice(0, 10).replace(/-/g, '.')
}

export function CouponSection() {
  const couponsQuery = useAdminCoupons()
  const createCoupon = useCreateCoupon()
  const replaceCoupon = useReplaceCoupon()
  const deleteCoupon = useDeleteCoupon()

  const [code, setCode] = useState('')
  const [reward, setReward] = useState('')
  const [expiresAt, setExpiresAt] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [confirmingDeleteId, setConfirmingDeleteId] = useState<number | null>(null)
  const [feedback, setFeedback] = useState<Feedback | null>(null)

  const isEditing = editingId !== null
  const isSubmitting = createCoupon.isPending || replaceCoupon.isPending

  function resetForm() {
    setCode('')
    setReward('')
    setExpiresAt('')
    setEditingId(null)
  }

  function submit() {
    const body: AdminCouponRequest = {
      code: code.trim(),
      reward: reward.trim(),
      expiresAt, // "YYYY-MM-DD" verbatim — no KST conversion (D-01)
    }
    const onSuccess = (text: string) => () => {
      setFeedback({ kind: 'success', text })
      resetForm()
    }
    const onError = () => setFeedback({ kind: 'error', text: MUTATION_FAIL })

    if (editingId !== null) {
      replaceCoupon.mutate({ id: editingId, body }, { onSuccess: onSuccess('변경을 저장했어요.'), onError })
    } else {
      createCoupon.mutate(body, { onSuccess: onSuccess('쿠폰을 등록했어요.'), onError })
    }
  }

  function startEdit(coupon: Coupon) {
    setEditingId(coupon.id)
    setCode(coupon.code)
    setReward(coupon.reward)
    setExpiresAt(coupon.expiresAt.slice(0, 10)) // date-only prefill for <input type="date">
    setConfirmingDeleteId(null)
    setFeedback(null)
  }

  function confirmDelete(id: number) {
    deleteCoupon.mutate(id, {
      onSuccess: () => {
        setFeedback({ kind: 'success', text: '쿠폰을 삭제했어요.' })
        setConfirmingDeleteId(null)
        if (editingId === id) resetForm()
      },
      onError: () => setFeedback({ kind: 'error', text: MUTATION_FAIL }),
    })
  }

  // expiresAt is a "YYYY-MM-DD" string, so a lexicographic ascending sort is chronological (soonest first).
  const coupons = couponsQuery.data
    ? [...couponsQuery.data].sort((a, b) => a.expiresAt.localeCompare(b.expiresAt))
    : []

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl">쿠폰</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Inline create/edit form. Fields stacked, label↔input sm/8px, fields md/16px. */}
        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault()
            if (isSubmitting) return
            submit()
          }}
        >
          <div className="space-y-2">
            <label className="text-sm font-semibold" htmlFor="coupon-code">
              코드
            </label>
            <Input
              id="coupon-code"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              required
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold" htmlFor="coupon-reward">
              보상
            </label>
            <Input
              id="coupon-reward"
              value={reward}
              onChange={(event) => setReward(event.target.value)}
              required
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold" htmlFor="coupon-expires-at">
              만료일
            </label>
            <Input
              id="coupon-expires-at"
              type="date"
              value={expiresAt}
              onChange={(event) => setExpiresAt(event.target.value)}
              required
            />
          </div>

          <div className="flex items-center gap-2">
            <Button
              type="submit"
              disabled={
                isSubmitting || code.trim() === '' || reward.trim() === '' || expiresAt === ''
              }
            >
              {isEditing
                ? isSubmitting
                  ? '저장 중…'
                  : '변경 저장'
                : isSubmitting
                  ? '등록 중…'
                  : '쿠폰 등록'}
            </Button>
            {isEditing && (
              <Button type="button" variant="outline" onClick={resetForm} disabled={isSubmitting}>
                취소
              </Button>
            )}
          </div>
        </form>

        {feedback && (
          <p className={feedback.kind === 'error' ? 'text-destructive text-base' : 'text-base'}>
            {feedback.text}
          </p>
        )}

        {/* Coupon list — expires_at asc (soonest first). AsyncBoundary supplies loading/empty/error. */}
        <AsyncBoundary
          status={couponsQuery.status}
          isEmpty={coupons.length === 0}
          onRetry={() => couponsQuery.refetch()}
          emptyHeading="등록된 쿠폰이 없어요"
          emptyBody="위 폼에서 첫 쿠폰을 등록하면 목록에 표시됩니다."
          errorMessage={CONNECTION_ERROR}
        >
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>코드</TableHead>
                <TableHead>보상</TableHead>
                <TableHead>만료일</TableHead>
                <TableHead className="text-right">관리</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {coupons.map((coupon) => (
                <TableRow key={coupon.id}>
                  <TableCell className="font-semibold">{coupon.code}</TableCell>
                  <TableCell className="whitespace-normal">{coupon.reward}</TableCell>
                  <TableCell className="tabular-nums">{formatExpiry(coupon.expiresAt)}</TableCell>
                  <TableCell className="text-right">
                    {confirmingDeleteId === coupon.id ? (
                      // Inline 2-step delete confirm — replaces the row actions in place.
                      <div className="flex flex-wrap items-center justify-end gap-2">
                        <span className="text-muted-foreground text-sm">
                          이 쿠폰을 삭제할까요? 되돌릴 수 없습니다.
                        </span>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={() => confirmDelete(coupon.id)}
                          disabled={deleteCoupon.isPending}
                        >
                          삭제
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setConfirmingDeleteId(null)}
                          disabled={deleteCoupon.isPending}
                        >
                          취소
                        </Button>
                      </div>
                    ) : (
                      <div className="flex items-center justify-end gap-2">
                        <Button size="sm" variant="outline" onClick={() => startEdit(coupon)}>
                          수정
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={() => {
                            setConfirmingDeleteId(coupon.id)
                            setFeedback(null)
                          }}
                        >
                          삭제
                        </Button>
                      </div>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </AsyncBoundary>
      </CardContent>
    </Card>
  )
}
