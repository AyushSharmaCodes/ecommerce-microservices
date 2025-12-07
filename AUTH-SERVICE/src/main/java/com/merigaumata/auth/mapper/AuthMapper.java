package com.merigaumata.auth.mapper;

import com.merigaumata.auth.model.RegisterRequest;
import com.merigaumata.user.model.CreateUserRequest;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface AuthMapper {

    CreateUserRequest maptoCreateUserRequest(RegisterRequest request);
}
