select * from money_oper t 
  where status = 'template' and recurrence_id is null;

update money_oper set recurrence_id = 'a51bae29-d6e8-45f0-8306-08176075956a' where id = 'a383ec27-b558-456b-aecb-d9ddaf58cb0e'; 
  
select * from recurrence_oper where template_id = 'a383ec27-b558-456b-aecb-d9ddaf58cb0e';

update money_trns set id_temp = id, id = uuid_generate_v4() where id = recurrence_id;
update balance_changes bc 
  set oper_id = (select id from money_trns t where t.recurrence_id = bc.oper_id and t.id_temp = bc.oper_id)
  where oper_id in (select oper_id from money_trns where id_temp = recurrence_id);

update recurrence_oper set is_arc = false;

select * from balance_changes where id = 'c70db3e4-882b-4ad4-a5f1-0f0d0bfcb744';
select * from balance_changes order by made desc;
update balance_changes set value = -value where id = 'c70db3e4-882b-4ad4-a5f1-0f0d0bfcb744';

update money_oper_item i set bs_id = (
  select balance_sheet_id from money_trns where id = i.oper_id
);
select * from money_oper_item where abs(value) = 307.00;
delete from money_oper_item where oper_id is null;

select * from balance where id = '50bd7922-6ed2-42b8-efef-7eadfe587ca9';
update balance set value = 39365.31+159 where id = '50bd7922-6ed2-42b8-efef-7eadfe587ca9';



