ALTER TABLE meal_plan DROP CONSTRAINT meal_plan_guest_session_id_fkey;
ALTER TABLE meal_plan
    ADD CONSTRAINT meal_plan_guest_session_id_fkey
    FOREIGN KEY (guest_session_id) REFERENCES guest_session(id) ON DELETE CASCADE;
