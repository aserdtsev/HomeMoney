package ru.serdtsev.homemoney.account;

import org.springframework.data.repository.CrudRepository;
import ru.serdtsev.homemoney.account.model.Account;

import java.util.UUID;

public interface AccountRepository extends CrudRepository<Account, UUID> {
}
