package org.example.corporate.enquiry;

import org.example.corporate.mail.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnquiryService {

    private final EnquiryRepository repo;
    private final MailService mail;
    private final String enquiriesTo;

    public EnquiryService(
            EnquiryRepository repo,
            MailService mail,
            @Value("${app.mail.enquiries-to:enquiries@corporate-gifting.local}") String enquiriesTo
    ) {
        this.repo = repo;
        this.mail = mail;
        this.enquiriesTo = enquiriesTo;
    }

    @Transactional
    public EnquiryDto submit(EnquiryRequest req) {
        Enquiry e = new Enquiry();
        e.setName(req.name().trim());
        e.setEmail(req.email().trim().toLowerCase());
        e.setCompanyName(blankToNull(req.companyName()));
        e.setPhone(blankToNull(req.phone()));
        e.setMessage(req.message().trim());
        e.setEstimatedQuantity(req.estimatedQuantity());
        e.setOccasion(blankToNull(req.occasion()));
        e.setEventDate(req.eventDate());
        e.setBudgetRange(blankToNull(req.budgetRange()));
        e.setStatus(EnquiryStatus.NEW);

        Enquiry saved = repo.save(e);
        mail.sendIfEnabled(enquiriesTo, "New corporate gifting enquiry from " + saved.getName(), buildBody(saved));
        return EnquiryDto.from(saved);
    }

    private String buildBody(Enquiry e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(e.getName()).append('\n');
        sb.append("Email: ").append(e.getEmail()).append('\n');
        if (e.getCompanyName() != null) sb.append("Company: ").append(e.getCompanyName()).append('\n');
        if (e.getPhone() != null) sb.append("Phone: ").append(e.getPhone()).append('\n');
        if (e.getEstimatedQuantity() != null) sb.append("Estimated quantity: ").append(e.getEstimatedQuantity()).append('\n');
        if (e.getOccasion() != null) sb.append("Occasion: ").append(e.getOccasion()).append('\n');
        if (e.getEventDate() != null) sb.append("Event date: ").append(e.getEventDate()).append('\n');
        if (e.getBudgetRange() != null) sb.append("Budget: ").append(e.getBudgetRange()).append('\n');
        sb.append('\n').append("Message:").append('\n').append(e.getMessage()).append('\n');
        return sb.toString();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
