package fr.lpreaux.usermanager.domain.model;

import fr.lpreaux.usermanager.domain.model.valueobject.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

/**
 * Main domain entity representing a user.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class User {
    private final UserId id;
    private final Login login;
    private final Password password;
    private final Name lastName;
    private final FirstName firstName;
    private final BirthDate birthDate;
    private final List<Email> emails;
    private final List<PhoneNumber> phoneNumbers;

    /**
     * Creates a new user with the required information.
     */
    public static User create(Login login, Password password, Name lastName, FirstName firstName,
                              BirthDate birthDate, List<Email> emails,
                              List<PhoneNumber> phoneNumbers) {
        return User.builder()
                .id(UserId.generate())
                .login(login)
                .password(password)
                .lastName(lastName)
                .firstName(firstName)
                .birthDate(birthDate)
                .emails(emails != null ? List.copyOf(emails) : Collections.emptyList())
                .phoneNumbers(phoneNumbers != null ? List.copyOf(phoneNumbers) : Collections.emptyList())
                .build();
    }

    /**
     * Updates the personal information of the user.
     */
    public User updatePersonalInformation(Name lastName, FirstName firstName, BirthDate birthDate) {
        return this.toBuilder()
                .lastName(lastName)
                .firstName(firstName)
                .birthDate(birthDate)
                .build();
    }

    /**
     * Adds an email to the user's email list.
     */
    public User addEmail(Email email) {
        if (email == null) {
            return this;
        }
        var newEmailList = new java.util.ArrayList<>(this.emails);
        newEmailList.add(email);
        return this.toBuilder()
                .emails(Collections.unmodifiableList(newEmailList))
                .build();
    }

    /**
     * Removes an email from the user's email list.
     */
    public User removeEmail(Email email) {
        if (email == null) {
            return this;
        }
        var newEmailList = new java.util.ArrayList<>(this.emails);
        newEmailList.remove(email);
        return this.toBuilder()
                .emails(Collections.unmodifiableList(newEmailList))
                .build();
    }

    /**
     * Adds a phone number to the user's phone number list.
     */
    public User addPhoneNumber(PhoneNumber phoneNumber) {
        if (phoneNumber == null) {
            return this;
        }
        var newPhoneNumberList = new java.util.ArrayList<>(this.phoneNumbers);
        newPhoneNumberList.add(phoneNumber);
        return this.toBuilder()
                .phoneNumbers(Collections.unmodifiableList(newPhoneNumberList))
                .build();
    }

    /**
     * Removes a phone number from the user's phone number list.
     */
    public User removePhoneNumber(PhoneNumber phoneNumber) {
        if (phoneNumber == null) {
            return this;
        }
        var newPhoneNumberList = new java.util.ArrayList<>(this.phoneNumbers);
        newPhoneNumberList.remove(phoneNumber);
        return this.toBuilder()
                .phoneNumbers(Collections.unmodifiableList(newPhoneNumberList))
                .build();
    }

    /**
     * Updates the user's password.
     */
    public User updatePassword(Password newPassword) {
        return this.toBuilder()
                .password(newPassword)
                .build();
    }

    /**
     * Gets the emails as an unmodifiable list.
     */
    public List<Email> getEmails() {
        return Collections.unmodifiableList(emails);
    }

    /**
     * Gets the phone numbers as an unmodifiable list.
     */
    public List<PhoneNumber> getPhoneNumbers() {
        return Collections.unmodifiableList(phoneNumbers);
    }
}