package ru.serdtsev.homemoney.user;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface UserAuthTokenRepository extends CrudRepository<UserAuthToken, UUID> {
}
