-- Atomic token-bucket consume + lazy refill (D-03).
-- One round trip does the whole read-refill-decrement-write so concurrent callers
-- cannot double-spend a token (the consume+refill is atomic — no Java-side race).
--
-- KEYS[1] = bucket hash key (global single bucket — one API key, D-03)
-- ARGV[1] = now_ms        (clock injected from Java so tests are deterministic)
-- ARGV[2] = capacity      (max tokens)
-- ARGV[3] = refill_per_sec (tokens regenerated per second)
-- ARGV[4] = requested     (tokens to consume this call)
-- returns 1 when granted, 0 when throttled.
--
-- NOTE: Redis converts Lua numbers passed to redis.call into INTEGERS (truncating
-- fractional tokens). Tokens accrue fractionally, so they are stored with tostring()
-- to preserve the float across calls.

local key = KEYS[1]
local now = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local refillPerSec = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local data = redis.call('HMGET', key, 'tokens', 'last_refill_ms')
local tokens = tonumber(data[1])
local lastRefill = tonumber(data[2])

if tokens == nil or lastRefill == nil then
  -- First ever access: bucket starts full.
  tokens = capacity
  lastRefill = now
end

local elapsed = now - lastRefill
if elapsed < 0 then elapsed = 0 end
tokens = math.min(capacity, tokens + (elapsed * refillPerSec / 1000.0))
lastRefill = now

local acquired = 0
if tokens >= requested then
  tokens = tokens - requested
  acquired = 1
end

redis.call('HMSET', key, 'tokens', tostring(tokens), 'last_refill_ms', tostring(lastRefill))
-- Bound key lifetime so abandoned buckets do not leak; refreshed on every access.
redis.call('PEXPIRE', key, 3600000)
return acquired
