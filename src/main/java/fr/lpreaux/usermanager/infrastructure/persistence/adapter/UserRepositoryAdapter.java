package fr.lpreaux.usermanager.infrastructure.persistence.adapter;

import fr.lpreaux.usermanager.application.port.out.UserRepository;
import fr.lpreaux.usermanager.domain.model.User;
import fr.lpreaux.usermanager.domain.model.valueobject.*;
import fr.lpreaux.usermanager.infrastructure.persistence.entity.UserEmailEntity;
import fr.lpreaux.usermanager.infrastructure.persistence.entity.UserEntity;
import fr.lpreaux.usermanager.infrastructure.persistence.entity.UserPhoneNumberEntity;
import fr.lpreaux.usermanager.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        UserEntity userEntity = mapToEntity(user);
        UserEntity savedEntity = userJpaRepository.save(userEntity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return userJpaRepository.findById(userId.getValue())
                .map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByLogin(Login login) {
        return userJpaRepository.findByLogin(login.getValue())
                .map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return userJpaRepository.findByEmail(email.getValue())
                .map(this::mapToDomain);
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll().stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UserId userId) {
        userJpaRepository.deleteById(userId.getValue());
    }

    @Override
    public boolean existsByLogin(Login login) {
        return userJpaRepository.existsByLogin(login.getValue());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return userJpaRepository.existsByEmail(email.getValue());
    }

    private UserEntity mapToEntity(User user) {
        UserEntity userEntity = UserEntity.builder()
                .id(user.getId().getValue())
                .login(user.getLogin().getValue())
                .password(user.getPassword().getValue())
                .lastName(user.getLastName().getValue())
                .firstName(user.getFirstName().getValue())
                .birthDate(user.getBirthDate().getValue())
                .build();

        // Map emails
        List<UserEmailEntity> emailEntities = user.getEmails().stream()
                .map(email -> UserEmailEntity.builder()
                        .email(email.getValue())
                        .user(userEntity)
                        .build())
                .collect(Collectors.toList());
        userEntity.setEmails(emailEntities);

        // Map phone numbers
        List<UserPhoneNumberEntity> phoneNumberEntities = user.getPhoneNumbers().stream()
                .map(phoneNumber -> UserPhoneNumberEntity.builder()
                        .phoneNumber(phoneNumber.getValue())
                        .user(userEntity)
                        .build())
                .collect(Collectors.toList());
        userEntity.setPhoneNumbers(phoneNumberEntities);

        return userEntity;
    }

    private User mapToDomain(UserEntity entity) {
        // Map emails
        List<Email> emails = entity.getEmails().stream()
                .map(emailEntity -> Email.of(emailEntity.getEmail()))
                .collect(Collectors.toList());

        // Map phone numbers
        List<PhoneNumber> phoneNumbers = entity.getPhoneNumbers().stream()
                .map(phoneNumberEntity -> PhoneNumber.of(phoneNumberEntity.getPhoneNumber()))
                .collect(Collectors.toList());

        return User.builder()
                .id(UserId.of(entity.getId()))
                .login(Login.of(entity.getLogin()))
                .password(Password.of(entity.getPassword()))
                .lastName(Name.of(entity.getLastName()))
                .firstName(FirstName.of(entity.getFirstName()))
                .birthDate(BirthDate.of(entity.getBirthDate()))
                .emails(emails)
                .phoneNumbers(phoneNumbers)
                .build();
    }
}