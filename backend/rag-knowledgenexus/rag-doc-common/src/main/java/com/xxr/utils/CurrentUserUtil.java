package com.xxr.utils;

import org.springframework.stereotype.Component;

/**
 * Provides access to the current request user without exposing ThreadLocal details.
 */
@Component
public class CurrentUserUtil {

    public void setCurrentId(Long id) {
        BaseContext.setCurrentId(id);
    }

    public Long getCurrentId() {
        return BaseContext.getCurrentId();
    }

    public void clearCurrentId() {
        BaseContext.removeCurrentId();
    }
}
