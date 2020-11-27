drop view public.v_crnt_saldo_by_base_cry;
CREATE OR REPLACE VIEW public.v_crnt_saldo_by_base_cry AS
  SELECT a.balance_sheet_id as bs_id, a.type, round(b.value * coalesce(er.ask, 1), 2) as saldo
  FROM
    accounts a,
    balances b
    LEFT JOIN exchange_rates er
      ON substring(er.id from 1 for 3) = b.currency_code
         and date = (select max(date) from exchange_rates er1 where substring(er1.id from 1 for 3) = b.currency_code)
  WHERE a.type in ('debit', 'credit', 'reserve', 'asset') and b.id = a.id;
COMMENT ON VIEW public.v_crnt_saldo_by_base_cry
IS 'Текущие балансы в базовой валюте';
