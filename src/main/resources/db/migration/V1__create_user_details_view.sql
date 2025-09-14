CREATE OR REPLACE VIEW v_user_details AS
SELECT
    u.id,
    u.username,
    u.email,
    u.role,
    u.status,
    u.created_at,
    up.first_name,
    up.last_name,
    up.phone_number,
    up.address
FROM
    users u
JOIN
    user_profiles up ON u.id = up.id;