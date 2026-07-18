package com.corporate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.corporate.entity.Enquiry;
import com.corporate.dto.CreateQuoteRequest;
import com.corporate.dto.EnquiryDto;
import com.corporate.dto.QuoteDto;
import com.corporate.dao.EnquiryRepository;
import com.corporate.entity.EnquiryStatus;
import com.corporate.service.QuoteService;
import com.corporate.web.NotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/enquiries")
public class AdminEnquiryController {

    private final EnquiryRepository repo;
    private final QuoteService quoteService;

    public AdminEnquiryController(EnquiryRepository repo, QuoteService quoteService) {
        this.repo = repo;
        this.quoteService = quoteService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<EnquiryDto> list() {
        return repo.findAllByOrderByCreatedAtDesc().stream()
                .map(EnquiryDto::from)
                .toList();
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public EnquiryDto updateStatus(@PathVariable Long id,
                                   @Valid @RequestBody EnquiryStatusUpdateRequest req) {
        Enquiry e = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Enquiry not found: " + id));
        e.setStatus(req.status());
        return EnquiryDto.from(e);
    }

    public record EnquiryStatusUpdateRequest(@NotNull EnquiryStatus status) {}

    @PostMapping("/{id}/quote")
    public QuoteDto createQuote(@PathVariable Long id,
                                @Valid @RequestBody CreateQuoteRequest req) {
        return quoteService.createQuote(id, req);
    }

    @GetMapping("/{id}/quote")
    public QuoteDto getQuote(@PathVariable Long id) {
        return quoteService.getByEnquiry(id);
    }
}
