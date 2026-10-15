package com.cinema.controller;

// NOTE: This class intentionally kept as a plain non-@Controller class.
// The GET / handler was moved to AuthController.home(HttpSession).
// Keeping this file prevents import/reference errors from any stale references.
// If no other code references this class it can be safely deleted.
public class HomeController {
    // Home handler moved to AuthController to avoid ambiguous mapping.
}
