INSERT INTO categories (name)
VALUES ('food'),
       ('transport'),
       ('other')
    ON CONFLICT (name) DO NOTHING;