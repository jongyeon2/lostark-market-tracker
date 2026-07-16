import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import type { MarketDir, MarketSort } from '@/lib/api'

/*
  정렬 선택 — 사용자 결정: 최저가·최근거래가 각 높은/낮은순(2기준 × 2방향 = 4옵션). 기준과 방향을 한
  select로 합친다. 값은 백엔드가 화이트리스트로 받는 vocabulary 그대로("min_price"/"recent_price",
  "asc"/"desc") — 잘못된 값은 백엔드에서 400이지만, 이 select는 유효한 4개만 낸다.
*/
export type SortValue = `${MarketSort}:${MarketDir}`

const OPTIONS: { value: SortValue; label: string }[] = [
  { value: 'min_price:asc', label: '최저가 낮은순' },
  { value: 'min_price:desc', label: '최저가 높은순' },
  { value: 'recent_price:asc', label: '최근거래가 낮은순' },
  { value: 'recent_price:desc', label: '최근거래가 높은순' },
]

export function parseSortValue(value: SortValue): { sort: MarketSort; dir: MarketDir } {
  const [sort, dir] = value.split(':') as [MarketSort, MarketDir]
  return { sort, dir }
}

export function SortSelect({
  value,
  onChange,
}: {
  value: SortValue
  onChange: (value: SortValue) => void
}) {
  return (
    <Select value={value} onValueChange={(v) => onChange(v as SortValue)}>
      <SelectTrigger className="w-44" aria-label="정렬 기준">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {OPTIONS.map((o) => (
          <SelectItem key={o.value} value={o.value}>
            {o.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
