package fr.lpreaux.usermanager.application.service;

import fr.lpreaux.usermanager.application.exception.*;
import fr.lpreaux.usermanager.application.port.in.DeleteUserUseCase;
import fr.lpreaux.usermanager.application.port.in.RegisterUserUseCase;
import fr.lpreaux.usermanager.application.port.in.UpdateUserUseCase;
import fr.lpreaux.usermanager.application.port.in.UserQueryUseCase;
import fr.lpreaux.usermanager.application.port.out.UserRepository;
import fr.lpreaux.usermanager.domain.model.User;
import fr.lpreaux.usermanager.domain.model.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementing all user-related use cases.
 * Acts as the central point for application logic, orchestrating domain objects and repositories.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService implements RegisterUserUseCase, UserQueryUseCase, UpdateUserUseCase, DeleteUserUseCase {

    private final UserRepository userRepository;

    /**
     * Registers a new user with the provided information.
     */
    @Override
    public UserId registerUser(RegisterUserCommand command) throws UserAlreadyExistsException {
        log.info("Registering new user with login: {}", command.login());

        // Validate login uniqueness
        if (userRepository.existsByLogin(Login.of(command.login()))) {
            log.warn("Registration failed: Login already exists: {}", command.login());
            throw new UserAlreadyExistsException("Login already exists: " + command.login());
        }

        // Convert and validate email uniqueness
        List<Email> emails = convertEmails(command.emails());
        for (Email email : emails) {
            if (userRepository.existsByEmail(email)) {
                log.warn("Registration failed: Email already exists: {}", email.getValue());
                throw new UserAlreadyExistsException("Email already exists: " + email.getValue());
            }
        }

        // Convert phone numbers
        List<PhoneNumber> phoneNumbers = convertPhoneNumbers(command.phoneNumbers());

        // Create user
        User user = User.create(
                Login.of(command.login()),
                Password.hash(command.password()),
                Name.of(command.lastName()),
                FirstName.of(command.firstName()),
                BirthDate.of(command.birthDate()),
                emails,
                phoneNumbers
        );

        // Save user
        User savedUser = userRepository.save(user);
        log.info("User successfully registered with ID: {}", savedUser.getId().getValue());

        return savedUser.getId();
    }

    /**
     * Finds a user by their ID.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<UserDetailsDTO> findUserById(String userId) {
        log.debug("Finding user by ID: {}", userId);

        return userRepository.findById(UserId.of(userId))
                .map(this::mapToUserDetailsDTO);
    }

    /**
     * Finds a user by their login.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<UserDetailsDTO> findUserByLogin(String login) {
        log.debug("Finding user by login: {}", login);

        return userRepository.findByLogin(Login.of(login))
                .map(this::mapToUserDetailsDTO);
    }

    /**
     * Finds a user by their email.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<UserDetailsDTO> findUserByEmail(String email) {
        log.debug("Finding user by email: {}", email);

        return userRepository.findByEmail(Email.of(email))
                .map(this::mapToUserDetailsDTO);
    }

    /**
     * Gets all users in the system.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserDetailsDTO> getAllUsers() {
        log.debug("Getting all users");

        return userRepository.findAll().stream()
                .map(this::mapToUserDetailsDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates a user's personal information.
     */
    @Override
    public void updatePersonalInfo(UpdatePersonalInfoCommand command) throws UserNotFoundException {
        log.info("Updating personal info for user ID: {}", command.userId());

        User user = findUserOrThrow(command.userId());

        User updatedUser = user.updatePersonalInformation(
                Name.of(command.lastName()),
                FirstName.of(command.firstName()),
                BirthDate.of(command.birthDate())
        );

        userRepository.save(updatedUser);
        log.info("Personal information updated for user ID: {}", command.userId());
    }

    /**
     * Changes a user's password.
     */
    @Override
    public void changePassword(ChangePasswordCommand command) throws UserNotFoundException, InvalidPasswordException {
        log.info("Changing password for user ID: {}", command.userId());

        User user = findUserOrThrow(command.userId());

        // Verify current password
        if (!user.getPassword().matches(command.currentPassword())) {
            log.warn("Password change failed: Current password is incorrect for user ID: {}", command.userId());
            throw new InvalidPasswordException("Current password is incorrect");
        }

        User updatedUser = user.updatePassword(Password.hash(command.newPassword()));
        userRepository.save(updatedUser);

        log.info("Password changed successfully for user ID: {}", command.userId());
    }

    /**
     * Adds an email to a user.
     */
    @Override
    public void addEmail(AddEmailCommand command) throws UserNotFoundException, EmailAlreadyExistsException {
        log.info("Adding email: {} to user ID: {}", command.email(), command.userId());

        Email email = Email.of(command.email());

        // Check if email already exists for another user
        if (userRepository.existsByEmail(email)) {
            log.warn("Add email failed: Email already exists: {}", command.email());
            throw new EmailAlreadyExistsException("Email already exists: " + command.email());
        }

        User user = findUserOrThrow(command.userId());
        User updatedUser = user.addEmail(email);

        userRepository.save(updatedUser);
        log.info("Email added successfully for user ID: {}", command.userId());
    }

    /**
     * Removes an email from a user.
     */
    @Override
    public void removeEmail(RemoveEmailCommand command) throws UserNotFoundException, LastEmailException {
        log.info("Removing email: {} from user ID: {}", command.email(), command.userId());

        User user = findUserOrThrow(command.userId());

        // Check if this is the user's last email
        if (user.getEmails().size() <= 1) {
            log.warn("Remove email failed: Cannot remove the last email for user ID: {}", command.userId());
            throw new LastEmailException("Cannot remove the last email address");
        }

        User updatedUser = user.removeEmail(Email.of(command.email()));
        userRepository.save(updatedUser);

        log.info("Email removed successfully for user ID: {}", command.userId());
    }

    /**
     * Adds a phone number to a user.
     */
    @Override
    public void addPhoneNumber(AddPhoneNumberCommand command) throws UserNotFoundException {
        log.info("Adding phone number: {} to user ID: {}", command.phoneNumber(), command.userId());

        User user = findUserOrThrow(command.userId());
        User updatedUser = user.addPhoneNumber(PhoneNumber.of(command.phoneNumber()));

        userRepository.save(updatedUser);
        log.info("Phone number added successfully for user ID: {}", command.userId());
    }

    /**
     * Removes a phone number from a user.
     */
    @Override
    public void removePhoneNumber(RemovePhoneNumberCommand command) throws UserNotFoundException {
        log.info("Removing phone number: {} from user ID: {}", command.phoneNumber(), command.userId());

        User user = findUserOrThrow(command.userId());
        User updatedUser = user.removePhoneNumber(PhoneNumber.of(command.phoneNumber()));

        userRepository.save(updatedUser);
        log.info("Phone number removed successfully for user ID: {}", command.userId());
    }

    /**
     * Deletes a user from the system.
     */
    @Override
    public void deleteUser(String userId) throws UserNotFoundException {
        log.info("Deleting user with ID: {}", userId);

        User user = findUserOrThrow(userId);
        userRepository.delete(user.getId());

        log.info("User deleted successfully with ID: {}", userId);
    }

    /**
     * Helper method to find a user by ID or throw an exception.
     */
    private User findUserOrThrow(String userId) throws UserNotFoundException {
        return userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UserNotFoundException("User not found with ID: " + userId);
                });
    }

    /**
     * Maps a domain User to a UserDetailsDTO.
     */
    private UserDetailsDTO mapToUserDetailsDTO(User user) {
        return new UserDetailsDTO(
                user.getId().getValue().toString(),
                user.getLogin().getValue(),
                user.getLastName().getValue(),
                user.getFirstName().getValue(),
                user.getBirthDate().getValue().toString(),
                user.getBirthDate().getAge(),
                user.getBirthDate().isAdult(),
                user.getEmails().stream().map(Email::getValue).collect(Collectors.toList()),
                user.getPhoneNumbers().stream().map(PhoneNumber::getValue).collect(Collectors.toList())
        );
    }

    /**
     * Helper method to convert string emails to Email objects.
     */
    private List<Email> convertEmails(List<String> emails) {
        return emails.stream()
                .map(Email::of)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to convert string phone numbers to PhoneNumber objects.
     */
    private List<PhoneNumber> convertPhoneNumbers(List<String> phoneNumbers) {
        return phoneNumbers.stream()
                .map(PhoneNumber::of)
                .collect(Collectors.toList());
    }
}