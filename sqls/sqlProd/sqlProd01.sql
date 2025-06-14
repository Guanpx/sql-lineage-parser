SELECT substr(apply_date, 1, 7) AS apply_month
	, CASE
		WHEN tacticskq_name LIKE '%第一钱包' THEN '第一钱包'
		WHEN tacticskq_name LIKE '%次级回捞%' THEN '次级回捞'
		WHEN tacticskq_name LIKE '%非会员%' THEN '非会员'
		WHEN tacticskq_name LIKE '%会员%' THEN '会员'
		WHEN tacticskq_name LIKE '%结清%' THEN '结清'
		WHEN tacticskq_name LIKE '%中原%' THEN '中原'
		ELSE '其他'
	END AS tacticskq_name_new, cust_level_final, sum(net_loan_amount) AS gmv
	, sum(total_periods * net_loan_amount) AS fq_gmv
	, SUM(CASE
		WHEN if_have_31day_performance = 1 THEN loan_amount
		ELSE 0
	END) AS fpd31_fm
	, SUM(CASE
		WHEN dpd31 = 1 THEN loan_amount
		ELSE 0
	END) AS fpd31_fz
	, SUM(CASE
		WHEN if_have_30day_performance = 1 THEN loan_amount
		ELSE 0
	END) AS fpd30_fm
	, SUM(CASE
		WHEN dpd30 = 1 THEN loan_amount
		ELSE 0
	END) AS fpd30_fz
	, SUM(CASE
		WHEN if_have_15day_performance = 1 THEN loan_amount
		ELSE 0
	END) AS fpd15_fm
	, SUM(CASE
		WHEN dpd15 = 1 THEN loan_amount
		ELSE 0
	END) AS fpd15_fz
	, SUM(CASE
		WHEN if_have_10day_performance = 1 THEN loan_amount
		ELSE 0
	END) AS fpd10_fm
	, SUM(CASE
		WHEN dpd10 = 1 THEN loan_amount
		ELSE 0
	END) AS fpd10_fz
	, SUM(CASE
		WHEN if_have_8day_performance = 1 THEN loan_amount
		ELSE 0
	END) AS fpd8_fm
	, SUM(CASE
		WHEN dpd8 = 1 THEN loan_amount
		ELSE 0
	END) AS fpd8_fz
	, SUM(CASE
		WHEN if_have_7day_performance = 1 THEN loan_amount
		ELSE 0
	END) AS fpd7_fm
	, SUM(CASE
		WHEN dpd7 = 1 THEN loan_amount
		ELSE 0
	END) AS fpd7_fz
	, SUM(CASE
		WHEN if_have_4day_performance = 1 THEN loan_amount
		ELSE 0
	END) AS fpd4_fm
	, SUM(CASE
		WHEN dpd4 = 1 THEN loan_amount
		ELSE 0
	END) AS fpd4_fz
	, SUM(CASE
		WHEN if_have_2day_performance = 1 THEN loan_amount
		ELSE 0
	END) AS fpd2_fm
	, SUM(CASE
		WHEN dpd2 = 1 THEN loan_amount
		ELSE 0
	END) AS fpd2_fz
	, SUM(CASE
		WHEN if_have_1day_performance = 1 THEN loan_amount
		ELSE 0
	END) AS fpd1_fm
	, SUM(CASE
		WHEN dpd1 = 1 THEN loan_amount
		ELSE 0
	END) AS fpd1_fz
	, SUM(CASE
		WHEN CAST(dpd AS int) >= 30 THEN CAST(loan_balanceb AS double)
		ELSE 0
	END) AS dpd30_mob3_fz
	, SUM(CAST(loan_amount_vintage AS double)) AS dpd30_mob3_fm
FROM (
	SELECT a.*
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) >= 31 THEN 1
			ELSE 0
		END AS if_have_31day_performance
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) < 31 THEN NULL
			WHEN b.actual_overdue_days >= 31 THEN 1
			ELSE 0
		END AS dpd31
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) >= 30 THEN 1
			ELSE 0
		END AS if_have_30day_performance
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) < 30 THEN NULL
			WHEN b.actual_overdue_days >= 30 THEN 1
			ELSE 0
		END AS dpd30
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) >= 15 THEN 1
			ELSE 0
		END AS if_have_15day_performance
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) < 15 THEN NULL
			WHEN b.actual_overdue_days >= 15 THEN 1
			ELSE 0
		END AS dpd15
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) >= 10 THEN 1
			ELSE 0
		END AS if_have_10day_performance
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) < 10 THEN NULL
			WHEN b.actual_overdue_days >= 10 THEN 1
			ELSE 0
		END AS dpd10
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) >= 8 THEN 1
			ELSE 0
		END AS if_have_8day_performance
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) < 8 THEN NULL
			WHEN b.actual_overdue_days >= 8 THEN 1
			ELSE 0
		END AS dpd8
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) >= 7 THEN 1
			ELSE 0
		END AS if_have_7day_performance
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) < 7 THEN NULL
			WHEN b.actual_overdue_days >= 7 THEN 1
			ELSE 0
		END AS dpd7
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) >= 4 THEN 1
			ELSE 0
		END AS if_have_4day_performance
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) < 4 THEN NULL
			WHEN b.actual_overdue_days >= 4 THEN 1
			ELSE 0
		END AS dpd4
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) >= 2 THEN 1
			ELSE 0
		END AS if_have_2day_performance
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) < 2 THEN NULL
			WHEN b.actual_overdue_days >= 2 THEN 1
			ELSE 0
		END AS dpd2
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) >= 1 THEN 1
			ELSE 0
		END AS if_have_1day_performance
		, CASE
			WHEN DATEDIFF(current_date(), b.repay_date) < 1 THEN NULL
			WHEN a.loan_type = 172
				AND (b.pre_settle_time IS NULL
					OR to_date(b.pre_settle_time) > current_date())
			THEN 1
			WHEN b.actual_overdue_days >= 1 THEN 1
			ELSE 0
		END AS dpd1, d.dpd, d.loan_balanceb, d.loan_amount AS loan_amount_vintage, m1.cust_level_final
	FROM (
		SELECT DISTINCT c.apply_date, c.user_id, c.order_no, c.order_status, c.cus_level
			, c.loan_amount, c.loan_period, c.refuse_son_name, c.department_name, c.channel_id
			, c.rule_sets_label, c.loan_type
			, CASE
				WHEN c.tacticskq_name LIKE '%非首贷%' THEN c.tacticskq_name
				ELSE '中原自营'
			END AS tacticskq_name, b.create_date AS cz_date, b.net_store_month_rate, b.net_loan_amount, b.total_periods
			, b.period_status
		FROM (
			SELECT *
			FROM dwd.dwd_beforeloan_order_examine_fd
			WHERE channel_user <> 1
		) c
			INNER JOIN (
				SELECT *
				FROM dwd.dwd_cap_repay_plan_fd
				WHERE channel_user <> 1
			) b
			ON c.order_no = b.order_no
				AND b.dt = to_date(date_sub(now(), 1))
				AND b.period = 1
				AND b.period_status IN ('0', '1', '2', '10')
		WHERE c.apply_date >= '2023-06-01'
			AND c.dt = to_date(date_sub(now(), 1))
			AND c.channel_id = 1
			AND c.loan_type = 4
	) a
		INNER JOIN (
			SELECT *
			FROM dwd.dwd_cap_repay_plan_fd
			WHERE channel_user <> 1
		) b
		ON a.order_no = b.order_no
			AND b.dt = to_date(date_sub(now(), 1))
			AND b.period = 1
			AND b.net_loan_amount > 0
		LEFT JOIN (
			SELECT cust_id, customer_level, order_id, order_date, mob
				, dpd, loan_amount, loan_time, loan_mob, loan_term
				, loan_balanceb, data_date, loan_type, tacticskq_name, dt
			FROM dws.dws_order_repay_info_vintage_im
			WHERE dt >= '2022-03-01'
				AND to_date(order_date) >= '2022-01-01'
				AND CAST(mob AS int) = 3
			UNION ALL
			SELECT cust_id, customer_level, order_id, order_date, mob
				, dpd, loan_amount, loan_time, loan_mob, loan_term
				, loan_balanceb, data_date, loan_type, tacticskq_name, dt
			FROM dws.dws_order_repay_info_vintage_fd
			WHERE dt = to_date(date_sub(now(), 1))
				AND dt <> to_date(last_Day(months_add(now(), -1)))
				AND to_date(order_date) >= '2022-01-01'
				AND CAST(mob AS int) = 3
		) d
		ON a.order_no = d.order_id
		LEFT JOIN dm_f_02.zl_test_model_hs_1031_step05 m1 ON a.user_id = m1.user_id
	WHERE a.apply_date >= '2023-06-01'
) final
GROUP BY 1, 2, 3
LIMIT 1000