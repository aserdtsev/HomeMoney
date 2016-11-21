CREATE OR REPLACE VIEW public.v_crnt_saldo_by_types AS
  SELECT a.balance_sheet_id, a.type, round(sum(b.value * coalesce(er.ask, 1)), 2) as saldo
  FROM
    accounts a,
    balances b
    LEFT JOIN exchange_rates er ON substring(er.id from 1 for 3) = b.currency_code
  WHERE a.type in ('debit', 'credit', 'reserve', 'asset') and b.id = a.id
  GROUP BY a.balance_sheet_id, a.type;
COMMENT ON VIEW public.v_crnt_saldo_by_types
IS 'Текущие балансы, сгруппированные по типам счетов';

CREATE OR REPLACE VIEW public.v_crnt_saldo_by_base_cry AS
  SELECT a.balance_sheet_id as bs_id, a.type, round(b.value * coalesce(er.ask, 1), 2) as saldo
  FROM
    accounts a,
    balances b
    LEFT JOIN exchange_rates er
      ON substring(er.id from 1 for 3) = b.currency_code
         and date = (select max(date) from exchange_rates er1 where substring(er.id from 1 for 3) = b.currency_code)
  WHERE a.type in ('debit', 'credit', 'reserve', 'asset') and b.id = a.id;
COMMENT ON VIEW public.v_crnt_saldo_by_base_cry
IS 'Текущие балансы в базовой валюте';

create or replace view public.v_trns_by_base_crn AS
  select
    mt.balance_sheet_id as bs_id,
    mt.status,
    mt.trn_date,
    mt.period,
    mt.templ_id,
    af.id as from_acc_id,
    af.type as from_acc_type,
    at.id as to_acc_id,
    at.type as to_acc_type,
    round(mt.amount * coalesce(erf.ask, coalesce(ert.ask, 1)), 2) as amount
  from
    money_trns mt,
    accounts af
    left join balances bf on bf.id = af.id
    left join exchange_rates erf on substring(erf.id from 1 for 3) = bf.currency_code,
    accounts at
    left join balances bt on bt.id = at.id
    left join exchange_rates ert on substring(ert.id from 1 for 3) = bt.currency_code
  where af.id = mt.from_acc_id and at.id = mt.to_acc_id;
COMMENT ON VIEW public.v_trns_by_base_crn
  IS 'Операции в базовой валюте';
