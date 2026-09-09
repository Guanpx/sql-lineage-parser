    INSERT INTO table aaa
    select
        order_id,
        period,
        common_day,
        case
        WHEN common_state>50 or ( common_state>20 AND common_state<30 ) THEN common_amount
        else 0 end as `代偿金额`,
        common_state,
        case
            WHEN common_state>50 THEN "买断"
            WHEN common_state>20 AND common_state<30 THEN "理赔"
        else "未代偿" end as `理赔状态`,
        case
        WHEN common_state>50 or ( common_state>20 AND common_state<30 ) THEN principal
        else 0 end as `理赔本金`,
        case
        WHEN common_state>50 or ( common_state>20 AND common_state<30 ) THEN round(common_amount-principal,2)
        else 0 end as `理赔利息`,
        `账单状态`
    from dw_fms_net_repay_plan_view pp