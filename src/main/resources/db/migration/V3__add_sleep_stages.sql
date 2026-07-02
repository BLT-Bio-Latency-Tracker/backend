-- 수면 단계 타임라인(구간별 시작~종료 시각)을 저장. 상세 화면의 "몇 시~몇 시 REM/깊은수면" 표시용.
-- JSONB 배열: [{"stage":"REM","startAt":"2026-05-30T02:13:00Z","endAt":"2026-05-30T02:41:00Z"}, ...]
ALTER TABLE sleep_records ADD COLUMN stages JSONB;
