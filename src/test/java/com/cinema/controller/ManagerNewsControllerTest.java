package com.cinema.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManagerNewsControllerTest {

    @Test
    void returnUrlMustStayOnTheNewsList() {
        assertEquals("/manager/news?status=PENDING&page=2", ManagerNewsController.safeReturn("/manager/news?status=PENDING&page=2"));
        assertEquals("/manager/news", ManagerNewsController.safeReturn("https://evil.example"));
        assertEquals("/manager/news", ManagerNewsController.safeReturn("//evil.example"));
        assertEquals("/manager/news", ManagerNewsController.safeReturn("/admin/dashboard"));
        assertEquals("/manager/news", ManagerNewsController.safeReturn(null));
    }
}
