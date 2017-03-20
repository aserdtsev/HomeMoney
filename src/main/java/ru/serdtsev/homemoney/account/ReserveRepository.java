package ru.serdtsev.homemoney.account;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface ReserveRepository  extends CrudRepository<Reserve, UUID> {
}
