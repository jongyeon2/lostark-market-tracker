-- V8: 쿠폰 시작일 추가 — 대시보드 쿠폰 행을 "시작일 ~ 만료일"로 표시하기 위해 (사용자 요청 2026-07-15).
--
-- NULLABLE인 이유: 이미 등록된 쿠폰에는 시작일이 없다. NOT NULL로 만들려면 기존 행에 무언가를
-- 채워야 하는데(created_at? 오늘?), 그건 관리자가 입력한 적 없는 날짜를 지어내는 것이다. 이 프로젝트는
-- 모르는 값을 그럴듯하게 채우지 않는다 — 백필 없이 null로 두고, 화면은 시작일이 있으면
-- "2026.07.01 ~ 2026.09.16", 없으면 "~ 2026.09.16"으로 정직하게 렌더한다.
--
-- 순서 보장(starts_at <= expires_at)은 CouponRequest의 @AssertTrue가 입력 단계에서 막는다. DB CHECK를
-- 쓰지 않는 이유는 기존 null 행과의 상호작용 없이 400 오류 계약(shared error contract)으로 되돌리는 게
-- 관리자에게 더 나은 피드백이기 때문. V1–V7은 불변(Flyway 체크섬 유지). PostgreSQL 타입만.
ALTER TABLE coupon
    ADD COLUMN starts_at DATE;
