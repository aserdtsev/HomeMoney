package ru.serdtsev.homemoney.account;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface BalanceRepository extends CrudRepository<Balance, UUID> {
}
