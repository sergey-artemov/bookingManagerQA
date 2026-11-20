package com.sergart.booking_manager_qa.ui;

import com.sergart.booking_manager_qa.db.Booking;
import com.sergart.booking_manager_qa.exception.BookingConflictException;
import com.sergart.booking_manager_qa.exception.NotFoundException;
import com.sergart.booking_manager_qa.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/bookings")
public class BookingPageController {

    private final BookingService bookingService;

    public BookingPageController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/bookings";
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        return "bookings";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("booking", new Booking());
        return "booking-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("booking") Booking booking,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "booking-form";
        }
        try {
            bookingService.create(booking);
        } catch (BookingConflictException e) {
            bindingResult.reject("overlap", e.getMessage());
            return "booking-form";
        }
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("booking", bookingService.findById(id));
        } catch (NotFoundException e) {
            return "redirect:/bookings";
        }
        return "booking-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("booking") Booking booking,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "booking-form";
        }
        booking.setId(id);
        try {
            bookingService.update(id, booking);
        } catch (BookingConflictException e) {
            bindingResult.reject("overlap", e.getMessage());
            return "booking-form";
        }
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        bookingService.delete(id);
        return "redirect:/bookings";
    }
}
