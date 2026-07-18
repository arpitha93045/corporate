package com.corporate.service;

import com.corporate.dao.EnquiryRepository;
import com.corporate.dao.ProductRepository;
import com.corporate.dao.QuoteRepository;
import com.corporate.dto.CreateQuoteRequest;
import com.corporate.dto.QuoteDto;
import com.corporate.entity.Enquiry;
import com.corporate.entity.EnquiryStatus;
import com.corporate.entity.Product;
import com.corporate.entity.Quote;
import com.corporate.entity.QuoteLine;
import com.corporate.entity.QuoteStatus;
import com.corporate.mail.MailService;
import com.corporate.web.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Issues and resolves quotes for enquiries (the RFQ workflow). Pricing is always
 * taken from the live catalog — the admin picks products + quantities, the server
 * decides money, mirroring {@link CheckoutService}. Lines are snapshotted so an
 * issued quote is immune to later product edits. Buyers act on a quote via an
 * opaque token (no login), same capability model as the agent draft cart.
 */
@Service
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

    /**
     * Allowed enquiry status transitions. Admin drives NEW→REVIEWING and issuing a
     * quote moves REVIEWING/NEW→QUOTED; the buyer's token page moves QUOTED→ACCEPTED
     * or QUOTED→DECLINED; an unactioned quote past its validity expires. CLOSED is a
     * terminal admin escape hatch reachable from anywhere.
     */
    private static final Map<EnquiryStatus, Set<EnquiryStatus>> ALLOWED = Map.of(
            EnquiryStatus.NEW,       EnumSet.of(EnquiryStatus.REVIEWING, EnquiryStatus.QUOTED, EnquiryStatus.CLOSED),
            EnquiryStatus.REVIEWING, EnumSet.of(EnquiryStatus.QUOTED, EnquiryStatus.CLOSED),
            EnquiryStatus.QUOTED,    EnumSet.of(EnquiryStatus.ACCEPTED, EnquiryStatus.DECLINED, EnquiryStatus.EXPIRED, EnquiryStatus.CLOSED),
            EnquiryStatus.ACCEPTED,  EnumSet.of(EnquiryStatus.CLOSED),
            EnquiryStatus.DECLINED,  EnumSet.of(EnquiryStatus.CLOSED),
            EnquiryStatus.EXPIRED,   EnumSet.of(EnquiryStatus.QUOTED, EnquiryStatus.CLOSED),
            EnquiryStatus.CLOSED,    EnumSet.noneOf(EnquiryStatus.class)
    );

    private final QuoteRepository quoteRepo;
    private final EnquiryRepository enquiryRepo;
    private final ProductRepository productRepo;
    private final MailService mail;
    private final String baseUrl;

    public QuoteService(QuoteRepository quoteRepo, EnquiryRepository enquiryRepo,
                        ProductRepository productRepo, MailService mail,
                        @Value("${app.base-url:http://localhost:4200}") String baseUrl) {
        this.quoteRepo = quoteRepo;
        this.enquiryRepo = enquiryRepo;
        this.productRepo = productRepo;
        this.mail = mail;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public QuoteDto createQuote(Long enquiryId, CreateQuoteRequest req) {
        Enquiry enquiry = enquiryRepo.findById(enquiryId)
                .orElseThrow(() -> new NotFoundException("Enquiry not found: " + enquiryId));

        // One active quote per enquiry — a new quote replaces any prior one.
        quoteRepo.findByEnquiryId(enquiryId).ifPresent(quoteRepo::delete);

        Quote quote = new Quote();
        quote.setEnquiry(enquiry);
        quote.setToken(UUID.randomUUID().toString());
        quote.setNotes(blankToNull(req.notes()));
        quote.setValidUntil(req.validUntil());
        quote.setStatus(QuoteStatus.SENT);

        long total = 0;
        for (CreateQuoteRequest.Line line : req.lines()) {
            Product p = productRepo.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + line.productId()));
            long unitPrice = p.getPriceCents();
            long lineTotal = unitPrice * line.quantity();
            QuoteLine ql = new QuoteLine();
            ql.setProduct(p);
            ql.setProductName(p.getName());
            ql.setUnitPriceCents(unitPrice);
            ql.setQuantity(line.quantity());
            ql.setLineTotalCents(lineTotal);
            quote.addLine(ql);
            total += lineTotal;
        }
        quote.setTotalCents(total);

        Quote saved = quoteRepo.save(quote);
        transition(enquiry, EnquiryStatus.QUOTED);

        mail.sendIfEnabled(enquiry.getEmail(),
                "Your corporate gifting quote is ready",
                buildBody(saved, enquiry));
        log.info("quote.created enquiry_id={} token={} lines={} total_cents={}",
                enquiryId, saved.getToken(), saved.getLines().size(), total);
        return QuoteDto.from(saved);
    }

    @Transactional(readOnly = true)
    public QuoteDto getByEnquiry(Long enquiryId) {
        Quote quote = quoteRepo.findByEnquiryId(enquiryId)
                .orElseThrow(() -> new NotFoundException("No quote for enquiry: " + enquiryId));
        return QuoteDto.from(quote);
    }

    @Transactional
    public QuoteDto fetchByToken(String token) {
        Quote quote = quoteRepo.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Quote not found"));
        expireIfLapsed(quote);
        return QuoteDto.from(quote);
    }

    @Transactional
    public QuoteDto respond(String token, boolean accept) {
        Quote quote = quoteRepo.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Quote not found"));
        expireIfLapsed(quote);
        if (quote.getStatus() != QuoteStatus.SENT) {
            throw new IllegalStateException("Quote " + token + " is " + quote.getStatus()
                    + " and can no longer be actioned");
        }
        quote.setStatus(accept ? QuoteStatus.ACCEPTED : QuoteStatus.DECLINED);
        transition(quote.getEnquiry(), accept ? EnquiryStatus.ACCEPTED : EnquiryStatus.DECLINED);
        log.info("quote.responded token={} accepted={}", token, accept);
        return QuoteDto.from(quote);
    }

    private void expireIfLapsed(Quote quote) {
        LocalDate validUntil = quote.getValidUntil();
        if (quote.getStatus() == QuoteStatus.SENT
                && validUntil != null && validUntil.isBefore(LocalDate.now())) {
            quote.setStatus(QuoteStatus.EXPIRED);
            if (quote.getEnquiry().getStatus() == EnquiryStatus.QUOTED) {
                transition(quote.getEnquiry(), EnquiryStatus.EXPIRED);
            }
            log.info("quote.expired token={}", quote.getToken());
        }
    }

    private void transition(Enquiry enquiry, EnquiryStatus target) {
        EnquiryStatus current = enquiry.getStatus();
        if (current == target) return;
        Set<EnquiryStatus> allowed = ALLOWED.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new IllegalStateException(
                    "Cannot transition enquiry " + enquiry.getId() + " from " + current + " to " + target);
        }
        enquiry.setStatus(target);
    }

    private String buildBody(Quote q, Enquiry e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(e.getName()).append(",\n\n");
        sb.append("Thanks for your enquiry");
        if (e.getOccasion() != null) sb.append(" about ").append(e.getOccasion());
        sb.append(". Here is your quote:\n\n");
        for (QuoteLine l : q.getLines()) {
            sb.append("- ").append(l.getProductName())
              .append("  x").append(l.getQuantity())
              .append("  ").append(formatCents(l.getLineTotalCents())).append('\n');
        }
        sb.append("\nTotal: ").append(formatCents(q.getTotalCents())).append('\n');
        if (q.getValidUntil() != null) sb.append("Valid until: ").append(q.getValidUntil()).append('\n');
        if (q.getNotes() != null) sb.append("\nNotes:\n").append(q.getNotes()).append('\n');
        sb.append("\nView and respond to your quote:\n")
          .append(baseUrl).append("/quote/").append(q.getToken()).append('\n');
        return sb.toString();
    }

    private static String formatCents(long cents) {
        return "₹" + (cents / 100.0);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
