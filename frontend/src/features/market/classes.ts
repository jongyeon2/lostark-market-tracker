/*
  아바타 직업 선택기의 직업군 분류 + 아이콘 매핑 (단일 출처).

  🔑 왜 하드코딩인가: 로스트아크 공식 API는 `GET /markets/options`에서 **한글 직업명 30개 문자열만**
  준다(실측 2026-07-20). 직업군(전사/무도가/…) 분류도, 아이콘 URL도 응답에 없다. 아이템 아이콘은
  API가 iconUrl을 주지만(그래서 GemCard·MarketRow는 데이터에서 파생한다) 직업은 줄 게 없다.

  아래 표는 추측이 아니라 **공식 웹사이트 자산에서 추출**했다:
  - 직업군↔클래스: `.../2018/obt/assets/js/pc.js` 의 `{root_ko, root_en, list:[{name_ko, name_en}]}`
  - 아이콘 파일명: `.../2018/obt/assets/css/pc.css` 의 `.icon--*` 배경 이미지 경로
  추출 결과 30개가 API의 30개와 1:1로 맞았다. 파일명은 규칙적이지 않아서 유추하면 404가 난다 —
  소울이터는 `souleater`(언더바 없음)인데 차원술사는 `dimension_master`(있음)이고,
  가디언나이트의 아이콘 파일은 `dragon_knight.svg`다.

  공식 데이터는 성별까지 쪼갠 11개 root(전사(남)/전사(여)/무도가(남)/…)지만, 화면에선 성별을 병합해
  7그룹으로 둔다. "전사(남)" 4개 + "전사(여)" 2개를 따로 세우면 3열 2줄 블록이 깨지고, 유저가 직업을
  찾는 단위는 성별이 아니라 전사/무도가다. 그룹 안의 구성원과 순서는 공식 그대로 유지한다.
*/

export interface ClassEntry {
  /** API가 주는 한글 직업명 그대로. 검색 요청에 그대로 실려 나가는 값이다. */
  name: string
  /** 공식 CDN의 `{slug}.svg` 파일명(→ `classIconUrl`). 매핑에 없는 신규 직업이면 null. */
  iconSlug: string | null
}

export interface ClassGroup {
  label: string
  classes: ClassEntry[]
}

/** 매핑에 없는 직업이 모이는 그룹. 아이콘 없이 이름만 뜬다 — 자세한 이유는 groupClasses 참고. */
const FALLBACK_GROUP_LABEL = '기타'

const OFFICIAL_GROUPS: readonly { label: string; classes: readonly ClassEntry[] }[] = [
  {
    label: '전사',
    classes: [
      { name: '디스트로이어', iconSlug: 'destroyer' },
      { name: '워로드', iconSlug: 'warlord' },
      { name: '버서커', iconSlug: 'berserker' },
      { name: '홀리나이트', iconSlug: 'holyknight' },
      { name: '슬레이어', iconSlug: 'slayer' },
      { name: '발키리', iconSlug: 'valkyrie' },
    ],
  },
  {
    label: '무도가',
    classes: [
      { name: '스트라이커', iconSlug: 'striker' },
      { name: '브레이커', iconSlug: 'breaker' },
      { name: '배틀마스터', iconSlug: 'battlemaster' },
      { name: '인파이터', iconSlug: 'infighter' },
      { name: '기공사', iconSlug: 'soulmaster' },
      { name: '창술사', iconSlug: 'lancemaster' },
    ],
  },
  {
    label: '헌터',
    classes: [
      { name: '데빌헌터', iconSlug: 'devilhunter' },
      { name: '블래스터', iconSlug: 'blaster' },
      { name: '호크아이', iconSlug: 'hawkeye' },
      { name: '스카우터', iconSlug: 'scouter' },
      { name: '건슬링어', iconSlug: 'gunslinger' },
    ],
  },
  {
    label: '마법사',
    classes: [
      { name: '바드', iconSlug: 'bard' },
      { name: '서머너', iconSlug: 'summoner' },
      { name: '아르카나', iconSlug: 'arcana' },
      // 소서리스의 아이콘 파일명만 elementalmaster다(구 명칭 '엘레멘탈마스터'의 잔재).
      { name: '소서리스', iconSlug: 'elementalmaster' },
    ],
  },
  {
    label: '암살자',
    classes: [
      { name: '블레이드', iconSlug: 'blade' },
      { name: '데모닉', iconSlug: 'demonic' },
      { name: '리퍼', iconSlug: 'reaper' },
      { name: '소울이터', iconSlug: 'souleater' },
    ],
  },
  {
    label: '스페셜리스트',
    classes: [
      { name: '도화가', iconSlug: 'artist' },
      { name: '기상술사', iconSlug: 'aeromancer' },
      { name: '환수사', iconSlug: 'wildsoul' },
      { name: '차원술사', iconSlug: 'dimension_master' },
    ],
  },
  {
    label: '가디언나이트',
    classes: [{ name: '가디언나이트', iconSlug: 'dragon_knight' }],
  },
]

/** 공식 CDN의 직업 아이콘 베이스. 아이템 아이콘(`iconUrl`)과 같은 호스트라 CSP에 이미 허용돼 있다. */
const CLASS_ICON_BASE = 'https://cdn-lostark.game.onstove.com/2018/obt/assets/images/common/class'

/*
  🔑 번들 → 핫링크로 바꾼 이유(2026-07-20): 저장소를 public으로 열기 때문이다.

  처음엔 SVG 30개를 public/class-icons/ 에 받아뒀다. 이 URL들은 API가 준 값이 아니라 사이트 내부
  자산(경로에 `2018/obt`가 박혀 있다)이라 예고 없이 옮겨질 수 있고, 번들해두면 그 위험이 없기
  때문이었다. 그 판단 자체는 지금도 맞다.

  바뀐 건 전제다. 오픈 API 이용약관 「지식재산권」은 "콘텐츠를 다운로드하여 보관하거나 보관된
  자료를 타인에게 제공하는 행위"를 위반으로 규정하고, 제재는 API 접근 권한 제한이다. private
  저장소일 땐 '보관'이었지만 public은 '타인에게 제공'이 된다 — 그리고 키가 정지되면 사이트가
  통째로 멈춘다. 링크가 깨지는 쪽이 훨씬 가벼운 사고라 트레이드오프가 뒤집혔다.

  깨짐 대비는 `ClassIcon`의 onError 폴백이 받는다(아이콘 자리 24px를 유지해 레이아웃이 안 밀린다).
*/
export function classIconUrl(iconSlug: string): string {
  return `${CLASS_ICON_BASE}/${iconSlug}.svg`
}

/**
 * API가 준 직업 목록을 화면에 그릴 직업군 블록으로 묶는다.
 *
 * **API 응답이 단일 출처다.** 위 표에만 있고 API에 없는 직업은 그리지 않는다(서버가 400을 낼 선택지를
 * 보여줄 이유가 없다). 반대로 **API에 새로 등장했는데 표에 없는 직업은 버리지 않고** `기타` 그룹에
 * 담는다 — 신규 직업이 출시된 날 그 직업만 조용히 선택 불가가 되는 게 최악이고, 아이콘이 없어도
 * 이름만으로 고를 수는 있다. 아이콘은 그때 SVG 한 개를 추가하면 따라온다.
 *
 * 구성원이 하나도 남지 않은 그룹은 빈 헤더를 남기지 않고 사라진다(대시보드 deriveCategories 선례).
 */
export function groupClasses(apiClasses: readonly string[]): ClassGroup[] {
  const available = new Set(apiClasses)
  const mapped = new Set<string>()

  const groups: ClassGroup[] = []
  for (const group of OFFICIAL_GROUPS) {
    const classes = group.classes.filter((c) => available.has(c.name))
    classes.forEach((c) => mapped.add(c.name))
    if (classes.length > 0) {
      groups.push({ label: group.label, classes })
    }
  }

  const unknown = apiClasses.filter((name) => !mapped.has(name))
  if (unknown.length > 0) {
    groups.push({
      label: FALLBACK_GROUP_LABEL,
      classes: unknown.map((name) => ({ name, iconSlug: null })),
    })
  }
  return groups
}
