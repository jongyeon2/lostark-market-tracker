/*
  테마 부트스트랩 — 첫 페인트 전에 <html>에 `dark`를 붙인다.

  왜 index.html 인라인이 아니라 별도 파일인가:
  운영 CSP가 `script-src 'self'`다(Caddyfile). 인라인 스크립트는 nonce도 hash도 없이는 차단되므로,
  인라인으로 두면 배포 환경에서만 조용히 실행되지 않는다 — 저장된 다크 설정이 새로고침마다 무시되고
  개발자는 로컬(CSP 없음)에서 재현조차 못 한다. 같은 이유로 폰트가 data URI로 인라인돼 차단된 적이
  있어(vite.config.ts의 assetsInlineLimit 주석) 같은 함정을 두 번 밟지 않는다.
  public/ 파일이라 동일 출처에서 서빙되고 `'self'`를 통과한다.

  왜 React가 아니라 여기인가:
  React 안에서 테마를 적용하면 번들을 받아 마운트할 때까지 라이트가 먼저 그려져 다크 사용자에게
  흰 화면이 번쩍한다(FOUC). <head>에서 defer 없이 불러 동기 실행되므로 본문 파싱 전에 끝난다.

  저장된 수동 선택이 있으면 그걸 쓰고, 없으면 OS 설정을 따른다(첫 방문 = 시스템).
  localStorage가 막힌 환경(프라이버시 모드 등)에서도 죽지 않게 try/catch로 감싼다 — 여기서 던지면
  테마 하나 때문에 페이지가 빈 채로 남는다.
*/
;(function () {
  try {
    var saved = localStorage.getItem('theme')
    var dark =
      saved === 'dark' ||
      (saved !== 'light' && window.matchMedia('(prefers-color-scheme: dark)').matches)
    if (dark) document.documentElement.classList.add('dark')
  } catch (e) {
    /* 저장소를 못 읽으면 라이트로 둔다 — 테마 하나 때문에 앱이 안 뜨는 것보다 낫다 */
  }
})()
