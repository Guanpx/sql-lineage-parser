  select
        id_no_des
        ,max(case when business = 'black' and feature = 'is_fraud_idcard' then feature_value else 0 end) as is_fraud_idcard
from  aaa