BEGIN;

INSERT INTO accounts (id, equity)
SELECT '3fa85f64-5717-4562-b3fc-2c963f66afa6'::uuid, 0
WHERE NOT EXISTS (
    SELECT 1 FROM accounts WHERE id = '3fa85f64-5717-4562-b3fc-2c963f66afa6'::uuid
);

-- Insert account_limits using table defaults if not exists and account is present
INSERT INTO account_limits (account_id)
SELECT '3fa85f64-5717-4562-b3fc-2c963f66afa6'::uuid
WHERE NOT EXISTS (
    SELECT 1 FROM account_limits WHERE account_id = '3fa85f64-5717-4562-b3fc-2c963f66afa6'::uuid
)
AND EXISTS (
    SELECT 1 FROM accounts WHERE id = '3fa85f64-5717-4562-b3fc-2c963f66afa6'::uuid
);

COMMIT;
