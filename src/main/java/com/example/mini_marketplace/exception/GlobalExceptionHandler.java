package com.example.mini_marketplace.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Custom exception: not enough stock when placing an order.
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
     * Illegal state transitions (e.g. wrong order status change).
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalState(IllegalStateException ex, Model model) {
        model.addAttribute("errorTitle",   "Operation Not Allowed");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/generic-error";
    }

    /**
     * Bean Validation failures triggered via @Validated on service/controller
     * method params (constraint violations at method level).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleConstraintViolation(ConstraintViolationException ex, Model model) {
        String details = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
        model.addAttribute("errorTitle",   "Validation Failed");
        model.addAttribute("errorMessage", details);
        return "error/generic-error";
    }

    /**
     * Catch-all fallback for any unhandled runtime exception.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model) {
        model.addAttribute("errorTitle",   "Unexpected Error");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/generic-error";
    }
}
