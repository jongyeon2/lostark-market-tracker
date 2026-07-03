import * as React from 'react'

import { cn } from '@/lib/utils'

// Hand-authored shadcn new-york `textarea` block (Windows shadcn CLI @-dir bug — UI-SPEC Registry Safety).
// Same convention as input.tsx; used for the optional 설명/분류 fields in the admin CRUD forms (D-07).
function Textarea({ className, ...props }: React.ComponentProps<'textarea'>) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        'border-input placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 aria-invalid:border-destructive flex field-sizing-content min-h-16 w-full rounded-md border bg-transparent px-3 py-2 text-base shadow-xs transition-[color,box-shadow] outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50 md:text-sm',
        className,
      )}
      {...props}
    />
  )
}

export { Textarea }
