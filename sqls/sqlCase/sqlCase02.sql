select id_no
      ,cast(avg(group_credit_amount) as string) as credit_amount_avg
      ,concat(substr(current_date(),1,7),'-01') as dt
      ,'quota' as business
      ,'credit_amount_avg' as feature
  from (select id_no,
               user_group_id,
               max(group_credit_amount) group_credit_amount
          from mid.mid_tempview
         group by id_no, user_group_id
         ) t
 group by id_no