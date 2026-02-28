package com.example.mini_marketplace.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles InsufficientStockException globally.
     * Renders a dedicated error page with stock details.
     */
    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleInsufficientStock(InsufficientStockException ex, Model model) {
        model.addAttribute("errorTitle",   "Not Enough Stock");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("productName",  ex.getProductName());
        model.addAttribute("requested",    ex.getRequested());
        model.addAttribute("available",    ex.getAvailable());
        return "error/stock-error";
    }

    /**
     * Handles illegal state transitions (e.g. wrong order status change).
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalState(IllegalStateException ex, Model model) {
        model.addAttribute("errorTitle",   "Operation Not Allowed");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/generic-error";
    }
}
