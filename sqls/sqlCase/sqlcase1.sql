SELECT CASE
        WHEN a = 1 THEN 11111
        WHEN a = 2 THEN 22222
        WHEN a = 3 THEN 33333
        ELSE -9999
    END AS ttt, u.rere, u.*
FROM table_aaa u