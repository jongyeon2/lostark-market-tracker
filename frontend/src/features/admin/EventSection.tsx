import { useState } from 'react'

import { AsyncBoundary } from '@/components/state/AsyncBoundary'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { formatKst } from '@/lib/formatKst'
import { kstLocalToUtcIso, utcIsoToKstLocal } from '@/lib/kstDatetime'
import { useAdminEvents, useCreateEvent, useDeleteEvent, useReplaceEvent } from '@/lib/queries'
import type { AdminEventRequest, EventType, GameEventResponse } from '@/lib/schemas'

/*
  게임 이벤트 CRUD section (ADMINUI-03), replacing the 15-02 placeholder. An inline form (D-07 — no
  dialog) at the top creates (POST 201) / edits (PUT full-replace, 200); the list below (occurred_at
  desc) offers 수정 and a destructive 삭제 guarded by an inline 2-step confirm (D-09). 발생 시각 is
  entered in KST and converted to UTC '...Z' on submit via kstLocalToUtcIso (D-08). Every mutation
  invalidates ['admin-events'] → refetch (D-10, server state single source of truth) and shows inline
  success/failure copy; a 401 triggers the 15-02 global auto-logout (D-03).
*/

// UI-SPEC EventType labels — shown/selected in 한국어, sent/stored as the enum value.
const EVENT_TYPE_LABELS: Record<EventType, string> = {
  LOA_ON: '로아ON',
  MAJOR_UPDATE: '대규모 업데이트',
  SEASON_END: '시즌 종료',
  BALANCE_PATCH: '밸런스 패치',
}
const EVENT_TYPE_ORDER: EventType[] = ['LOA_ON', 'MAJOR_UPDATE', 'SEASON_END', 'BALANCE_PATCH']

const MUTATION_FAIL = '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.'
const CONNECTION_ERROR =
  '백엔드에 연결하지 못했어요. admin 백엔드(:8080)가 켜져 있는지 확인하고 다시 시도하세요.'

type Feedback = { kind: 'success' | 'error'; text: string }

export function EventSection() {
  const eventsQuery = useAdminEvents()
  const createEvent = useCreateEvent()
  const replaceEvent = useReplaceEvent()
  const deleteEvent = useDeleteEvent()

  const [eventType, setEventType] = useState<EventType>('LOA_ON')
  const [title, setTitle] = useState('')
  const [occurredAtKst, setOccurredAtKst] = useState('')
  const [description, setDescription] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [confirmingDeleteId, setConfirmingDeleteId] = useState<number | null>(null)
  const [feedback, setFeedback] = useState<Feedback | null>(null)

  const isEditing = editingId !== null
  const isSubmitting = createEvent.isPending || replaceEvent.isPending

  function resetForm() {
    setEventType('LOA_ON')
    setTitle('')
    setOccurredAtKst('')
    setDescription('')
    setEditingId(null)
  }

  function submit() {
    const body: AdminEventRequest = {
      eventType,
      title: title.trim(),
      occurredAt: kstLocalToUtcIso(occurredAtKst), // KST wall-clock → UTC '...Z' (D-08)
      description: description.trim() ? description.trim() : undefined,
    }
    const onSuccess = (text: string) => () => {
      setFeedback({ kind: 'success', text })
      resetForm()
    }
    const onError = () => setFeedback({ kind: 'error', text: MUTATION_FAIL })

    if (editingId !== null) {
      replaceEvent.mutate({ id: editingId, body }, { onSuccess: onSuccess('변경을 저장했어요.'), onError })
    } else {
      createEvent.mutate(body, { onSuccess: onSuccess('이벤트를 등록했어요.'), onError })
    }
  }

  function startEdit(event: GameEventResponse) {
    setEditingId(event.id)
    setEventType(event.eventType)
    setTitle(event.title)
    setOccurredAtKst(utcIsoToKstLocal(event.occurredAt)) // UTC → KST prefill (D-08)
    setDescription(event.description ?? '')
    setConfirmingDeleteId(null)
    setFeedback(null)
  }

  function confirmDelete(id: number) {
    deleteEvent.mutate(id, {
      onSuccess: () => {
        setFeedback({ kind: 'success', text: '이벤트를 삭제했어요.' })
        setConfirmingDeleteId(null)
        if (editingId === id) resetForm()
      },
      onError: () => setFeedback({ kind: 'error', text: MUTATION_FAIL }),
    })
  }

  // occurredAt is a UTC ISO '...Z' string, so a lexicographic descending sort is chronological (newest first).
  const events = eventsQuery.data
    ? [...eventsQuery.data].sort((a, b) => b.occurredAt.localeCompare(a.occurredAt))
    : []

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl">게임 이벤트</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Inline create/edit form (D-07). Fields stacked, label↔input sm/8px, fields md/16px. */}
        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault()
            if (isSubmitting) return
            submit()
          }}
        >
          <div className="space-y-2">
            <label className="text-sm font-semibold" htmlFor="event-type">
              이벤트 유형
            </label>
            <Select value={eventType} onValueChange={(value) => setEventType(value as EventType)}>
              <SelectTrigger id="event-type" className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {EVENT_TYPE_ORDER.map((type) => (
                  <SelectItem key={type} value={type}>
                    {EVENT_TYPE_LABELS[type]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold" htmlFor="event-title">
              제목
            </label>
            <Input
              id="event-title"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              required
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold" htmlFor="event-occurred-at">
              발생 시각 (KST)
            </label>
            <Input
              id="event-occurred-at"
              type="datetime-local"
              value={occurredAtKst}
              onChange={(event) => setOccurredAtKst(event.target.value)}
              required
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold" htmlFor="event-description">
              설명 (선택)
            </label>
            <Textarea
              id="event-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </div>

          <div className="flex items-center gap-2">
            <Button type="submit" disabled={isSubmitting || title.trim() === '' || occurredAtKst === ''}>
              {isEditing
                ? isSubmitting
                  ? '저장 중…'
                  : '변경 저장'
                : isSubmitting
                  ? '등록 중…'
                  : '이벤트 등록'}
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

        {/* Event list — occurred_at desc. AsyncBoundary supplies loading/empty/error (D-10). */}
        <AsyncBoundary
          status={eventsQuery.status}
          isEmpty={events.length === 0}
          onRetry={() => eventsQuery.refetch()}
          emptyHeading="등록된 이벤트가 없어요"
          emptyBody="위 폼에서 첫 게임 이벤트를 등록하면 목록에 표시됩니다."
          errorMessage={CONNECTION_ERROR}
        >
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>유형</TableHead>
                <TableHead>제목</TableHead>
                <TableHead>발생 시각</TableHead>
                <TableHead className="text-right">관리</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {events.map((event) => (
                <TableRow key={event.id}>
                  <TableCell className="font-semibold">{EVENT_TYPE_LABELS[event.eventType]}</TableCell>
                  <TableCell className="whitespace-normal">{event.title}</TableCell>
                  <TableCell className="tabular-nums">{formatKst(event.occurredAt)}</TableCell>
                  <TableCell className="text-right">
                    {confirmingDeleteId === event.id ? (
                      // Inline 2-step delete confirm (D-09) — replaces the row actions in place.
                      <div className="flex flex-wrap items-center justify-end gap-2">
                        <span className="text-muted-foreground text-sm">
                          이 이벤트를 삭제할까요? 되돌릴 수 없습니다.
                        </span>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={() => confirmDelete(event.id)}
                          disabled={deleteEvent.isPending}
                        >
                          삭제
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setConfirmingDeleteId(null)}
                          disabled={deleteEvent.isPending}
                        >
                          취소
                        </Button>
                      </div>
                    ) : (
                      <div className="flex items-center justify-end gap-2">
                        <Button size="sm" variant="outline" onClick={() => startEdit(event)}>
                          수정
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={() => {
                            setConfirmingDeleteId(event.id)
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
