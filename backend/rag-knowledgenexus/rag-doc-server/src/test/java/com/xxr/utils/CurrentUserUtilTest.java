package com.xxr.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CurrentUserUtilTest {

    private final CurrentUserUtil currentUserUtil = new CurrentUserUtil();

    @AfterEach
    void clearContext() {
        BaseContext.removeCurrentId();
    }

    @Test
    void getCurrentIdReturnsValueFromBaseContext() {
        BaseContext.setCurrentId(42L);

        assertEquals(42L, currentUserUtil.getCurrentId());
    }

    @Test
    void getCurrentIdReturnsNullWhenNoUserIsLoggedIn() {
        assertNull(currentUserUtil.getCurrentId());
    }

    @Test
    void setCurrentIdStoresUserInBaseContext() {
        currentUserUtil.setCurrentId(7L);

        assertEquals(7L, BaseContext.getCurrentId());
    }

    @Test
    void clearCurrentIdRemovesUserFromBaseContext() {
        BaseContext.setCurrentId(7L);

        currentUserUtil.clearCurrentId();

        assertNull(BaseContext.getCurrentId());
    }
}
