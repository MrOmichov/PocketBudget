INSERT INTO users (username, password_hash, enabled)
VALUES ('legacy', '!disabled', FALSE);

ALTER TABLE expenses
    ADD COLUMN user_id bigint;

UPDATE expenses
SET user_id = (SELECT id
               FROM users
               WHERE username = 'legacy');

ALTER TABLE expenses
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE expenses
    ADD CONSTRAINT expenses_user_fk
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE RESTRICT;

CREATE INDEX expenses_user_date_id_idx
    ON expenses (user_id, spent_at DESC, id DESC);