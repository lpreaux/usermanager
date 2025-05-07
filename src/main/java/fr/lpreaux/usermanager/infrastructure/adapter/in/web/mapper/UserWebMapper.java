package fr.lpreaux.usermanager.infrastructure.adapter.in.web.mapper;

import fr.lpreaux.usermanager.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import fr.lpreaux.usermanager.application.port.in.UpdateUserUseCase.*;
import fr.lpreaux.usermanager.application.port.in.UserQueryUseCase.UserDetailsDTO;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request.*;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.response.UserResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting between web DTOs and domain/application layer objects.
 */
@Component
public class UserWebMapper {

    /**
     * Converts a RegisterUserRequest to RegisterUserCommand.
     */
    public RegisterUserCommand toRegisterCommand(RegisterUserRequest request) {
        return new RegisterUserCommand(
                request.login(),
                request.password(),
                request.lastName(),
                request.firstName(),
                request.birthDate(),
                request.emails(),
                request.phoneNumbers() != null ? request.phoneNumbers() : List.of()
        );
    }

    /**
     * Converts UserDetailsDTO to UserResponse.
     */
    public UserResponse toUserResponse(UserDetailsDTO userDetails) {
        return new UserResponse(
                userDetails.id(),
                userDetails.login(),
                userDetails.lastName(),
                userDetails.firstName(),
                userDetails.birthDate(),
                userDetails.age(),
                userDetails.isAdult(),
                userDetails.emails(),
                userDetails.phoneNumbers()
        );
    }

    /**
     * Creates UpdatePersonalInfoCommand from request and user ID.
     */
    public UpdatePersonalInfoCommand toUpdatePersonalInfoCommand(String userId, UpdatePersonalInfoRequest request) {
        return new UpdatePersonalInfoCommand(
                userId,
                request.lastName(),
                request.firstName(),
                request.birthDate()
        );
    }

    /**
     * Creates ChangePasswordCommand from request and user ID.
     */
    public ChangePasswordCommand toChangePasswordCommand(String userId, ChangePasswordRequest request) {
        return new ChangePasswordCommand(
                userId,
                request.currentPassword(),
                request.newPassword()
        );
    }

    /**
     * Creates AddEmailCommand from request and user ID.
     */
    public AddEmailCommand toAddEmailCommand(String userId, AddEmailRequest request) {
        return new AddEmailCommand(
                userId,
                request.email()
        );
    }

    /**
     * Creates AddPhoneNumberCommand from request and user ID.
     */
    public AddPhoneNumberCommand toAddPhoneNumberCommand(String userId, AddPhoneNumberRequest request) {
        return new AddPhoneNumberCommand(
                userId,
                request.phoneNumber()
        );
    }
}
