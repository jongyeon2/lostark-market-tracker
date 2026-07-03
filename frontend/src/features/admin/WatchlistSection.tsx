import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

/*
  PLACEHOLDER — replaced by 15-04 (워치리스트 section, ADMINUI-04). Mounted here by the 15-02 shell so
  15-04 edits ONLY this file and never AdminConsolePage (wave-conflict avoidance). Until then it
  renders a calm "준비 중" card so the console shell reads complete.
*/
export function WatchlistSection() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl">워치리스트</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-muted-foreground text-base">준비 중입니다.</p>
      </CardContent>
    </Card>
  )
}
