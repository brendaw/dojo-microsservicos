package org.dojo.invoice.client;

import org.dojo.invoice.dto.ClientDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "client-service-client", url = "${spring.client-service.url}")
public interface ClientServiceClient {

    @GetMapping("/clients/legal-document/{legalDocument}")
    ClientDto getClientByLegalDocument(@PathVariable String legalDocument);

}