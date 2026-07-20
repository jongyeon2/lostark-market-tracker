import { useEffect, useState } from 'react'
import { Moon, Sun } from 'lucide-react'

import { cn } from '@/lib/utils'

/*
  테마 토글 (2단계, 사용자 결정 2026-07-20). 해/달 버튼 하나 — 첫 방문은 OS 설정을 따르고, 한 번
  누르면 그 선택을 localStorage에 기억한다.

  🔑 초기 상태를 여기서 계산하지 않는다. 실제 적용은 index.html의 인라인 스크립트가 첫 페인트 전에
  이미 끝냈으므로(FOUC 방지), 이 컴포넌트는 <html>에 붙은 결과를 **읽기만** 한다. 여기서 다시
  판단하면 두 곳이 같은 규칙을 중복으로 갖게 되고, 어긋나는 순간 화면과 버튼 아이콘이 반대가 된다.

  수동 선택 전에는 OS 설정 변경을 실시간으로 따라간다 — 저장값이 없을 때만. 이미 고른 사용자의
  선택을 OS가 덮으면 그건 버그다.
*/
const STORAGE_KEY = 'theme'

function isDarkNow(): boolean {
  return document.documentElement.classList.contains('dark')
}

function apply(dark: boolean) {
  document.documentElement.classList.toggle('dark', dark)
}

export function ThemeToggle() {
  const [dark, setDark] = useState(isDarkNow)

  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = (e: MediaQueryListEvent) => {
      // 저장값이 있으면 사용자가 이미 골랐다는 뜻 — OS 변경을 무시한다.
      try {
        if (localStorage.getItem(STORAGE_KEY) !== null) return
      } catch {
        return
      }
      apply(e.matches)
      setDark(e.matches)
    }
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [])

  function toggle() {
    const next = !dark
    apply(next)
    setDark(next)
    try {
      localStorage.setItem(STORAGE_KEY, next ? 'dark' : 'light')
    } catch {
      /* 저장 실패해도 이번 세션의 전환은 유효하다 — 기억만 안 될 뿐 */
    }
  }

  return (
    <button
      type="button"
      onClick={toggle}
      // 스크린리더에는 "무엇으로 바뀌는지"를 말한다. 아이콘도 같은 뜻(다크면 해 = 라이트로 감).
      aria-label={dark ? '라이트 모드로 전환' : '다크 모드로 전환'}
      aria-pressed={dark}
      className={cn(
        'text-muted-foreground hover:text-foreground hover:bg-muted focus-visible:ring-ring',
        'flex size-9 shrink-0 items-center justify-center rounded-md transition-colors',
        'focus-visible:ring-2 focus-visible:outline-none',
      )}
    >
      {dark ? (
        <Sun className="size-5" aria-hidden="true" />
      ) : (
        <Moon className="size-5" aria-hidden="true" />
      )}
    </button>
  )
}
