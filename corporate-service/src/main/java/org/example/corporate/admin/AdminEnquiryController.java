package org.example.corporate.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.example.corporate.enquiry.Enquiry;
import org.example.corporate.enquiry.EnquiryDto;
import org.example.corporate.enquiry.EnquiryRepository;
import org.example.corporate.enquiry.EnquiryStatus;
import org.example.corporate.web.NotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/enquiries")
public class AdminEnquiryController {

    private final EnquiryRepository repo;

    public AdminEnquiryController(EnquiryRepository repo) {
        this.repo = repo;
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
}
