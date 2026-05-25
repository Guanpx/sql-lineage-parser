SELECT user_id
    , dept_id
    , salary
    , ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rn
    , RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS dept_rank
    , SUM(salary) OVER (PARTITION BY dept_id) AS dept_total
    , LAG(salary, 1) OVER (PARTITION BY dept_id ORDER BY hire_date) AS prev_salary
FROM employees
WHERE dept_id IS NOT NULL
