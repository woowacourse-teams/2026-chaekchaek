package com.chaekchaek.admin.authorization;

import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminAuthorizationAspect {

    private final CurrentActorProvider currentActorProvider;

    @Before("@annotation(com.chaekchaek.admin.authorization.AdminOnly)")
    public void checkAdmin() {
        if (!currentActorProvider.getCurrentActor().isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
