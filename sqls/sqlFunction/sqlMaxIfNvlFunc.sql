select
    if(advance_or_overdue_days_30dr_mx IS NULL, -999999, advance_or_overdue_days_30dr_mx)
from aaa

select
max(if(feature='ubt_app_click_content_repay_cc_not_zero_30d', feature_value, null))
from aaa

select
nvl(jz_id_no_des_60dr_set, 'null')
from aaa
