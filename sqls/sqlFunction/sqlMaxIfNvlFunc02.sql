
select
    max(if(feature='ubt_app_click_content_repay_cc_not_zero_30d', feature_value, null))
from aaa
