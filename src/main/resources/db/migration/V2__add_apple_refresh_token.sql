-- 계정 삭제 시 Sign in with Apple 토큰 폐기(revoke)를 위해 refresh token을 보관.
-- 값은 애플리케이션 레벨 AES-256-GCM으로 암호화되어 저장된다(평문 아님).
ALTER TABLE users ADD COLUMN apple_refresh_token TEXT;
