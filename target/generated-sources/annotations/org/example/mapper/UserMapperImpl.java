package org.example.mapper;

import javax.annotation.processing.Generated;
import org.example.dto.request.UserRegistrationRequestDto;
import org.example.dto.response.UserProfileResponseDto;
import org.example.dto.response.UserRegistrationResponseDto;
import org.example.dto.response.UserSearchResponseDto;
import org.example.entity.User;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-10T00:40:34+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 19.0.1 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserRegistrationRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        User user = new User();

        user.setEmail( dto.email() );
        user.setPassword( dto.password() );
        user.setFirstName( dto.firstName() );
        user.setLastName( dto.lastName() );

        return user;
    }

    @Override
    public UserRegistrationResponseDto toRegistrationDto(User user) {
        if ( user == null ) {
            return null;
        }

        Long id = null;
        String email = null;
        String firstName = null;
        String lastName = null;

        id = user.getId();
        email = user.getEmail();
        firstName = user.getFirstName();
        lastName = user.getLastName();

        String nickName = null;

        UserRegistrationResponseDto userRegistrationResponseDto = new UserRegistrationResponseDto( id, email, firstName, lastName, nickName );

        return userRegistrationResponseDto;
    }

    @Override
    public UserProfileResponseDto toProfileDto(User user) {
        if ( user == null ) {
            return null;
        }

        Long id = null;
        String firstName = null;
        String lastName = null;
        String email = null;

        id = user.getId();
        firstName = user.getFirstName();
        lastName = user.getLastName();
        email = user.getEmail();

        String nickName = null;

        UserProfileResponseDto userProfileResponseDto = new UserProfileResponseDto( id, firstName, lastName, nickName, email );

        return userProfileResponseDto;
    }

    @Override
    public UserSearchResponseDto toSearchDto(User user) {
        if ( user == null ) {
            return null;
        }

        Long id = null;
        String firstName = null;
        String lastName = null;

        id = user.getId();
        firstName = user.getFirstName();
        lastName = user.getLastName();

        String nickName = null;

        UserSearchResponseDto userSearchResponseDto = new UserSearchResponseDto( id, firstName, lastName, nickName );

        return userSearchResponseDto;
    }
}
