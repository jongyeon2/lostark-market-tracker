import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

// shadcn class-merge helper used by every ui block.
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}



