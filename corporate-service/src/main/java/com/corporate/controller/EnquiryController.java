package com.corporate.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.corporate.dto.EnquiryDto;
import com.corporate.dto.EnquiryRequest;
import com.corporate.service.EnquiryService;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    private final EnquiryService service;

    public EnquiryController(EnquiryService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnquiryDto submit(@Valid @RequestBody EnquiryRequest req) {
        return service.submit(req);
    }
}
