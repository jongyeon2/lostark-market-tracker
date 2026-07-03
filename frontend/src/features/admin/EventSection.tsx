import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

/*
  PLACEHOLDER — replaced by 15-03 (게임 이벤트 CRUD section, ADMINUI-03). Mounted here by the 15-02 shell
  so 15-03 edits ONLY this file and never AdminConsolePage (wave-conflict avoidance). Until then it
  renders a calm "준비 중" card so the console shell reads complete.
*/
export function EventSection() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl">게임 이벤트</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-muted-foreground text-base">준비 중입니다.</p>
      </CardContent>
    </Card>
  )
}
