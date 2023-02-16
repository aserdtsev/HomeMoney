create or replace view public.v_crnt_saldo_by_base_cry AS
select a.balance_sheet_id, a.type, round(b.value * coalesce(er.ask, 1), 2) as saldo
from
    account a
        join balance b on b.id = a.id
        left join exchange_rate er
                  on substring(er.id from 1 for 3) = b.currency_code
                      and date = (select max(date) from exchange_rate er1 where substring(er1.id from 1 for 3) = b.currency_code)
where a.type in ('debit', 'credit', 'reserve', 'asset');
comment on view public.v_crnt_saldo_by_base_cry is 'Текущие балансы в базовой валюте';

