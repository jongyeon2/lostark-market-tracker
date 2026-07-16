import { useEffect, useState } from 'react'

/*
  검색어 디바운스. 사용자가 한 글자 칠 때마다 API를 때리면 폭주하므로, 입력이 멈춘 뒤 delay(기본 300ms)가
  지나야 값을 반영한다. 백엔드 5분 캐시가 반복 요청을 막고, 이 훅이 타이핑 폭주를 막는 이중 방어 —
  둘이 함께 레이트리밋 예산(수집기와 공유)을 지킨다.
*/
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(id)
  }, [value, delay])
  return debounced
}
