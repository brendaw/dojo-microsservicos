package org.dojo.invoice.service;

import feign.FeignException;
import org.dojo.invoice.client.ClientServiceClient;
import org.dojo.invoice.document.Invoice;
import org.dojo.invoice.document.Product;
import org.dojo.invoice.dto.ClientDto;
import org.dojo.invoice.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ClientServiceClient clientServiceClient;

    public Invoice issueInvoice(String legalDocument, List<Product> products) {
        ClientDto clientDto;

        try {
            clientDto = clientServiceClient.getClientByLegalDocument(legalDocument);
        } catch (FeignException ex) {
            if (HttpStatus.NOT_FOUND.value() == ex.status()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return invoiceRepository.save(Invoice.builder()
                    .legalDocument(legalDocument)
                    .clientName(clientDto.getName())
                    .products(products)
                    .totalAmount(sumAllProductsAmounts(products))
                    .build()
        );
    }

    public Invoice getInvoiceById(String invoiceId) {
        return invoiceRepository.findById(invoiceId).get();
    }

    public List<Invoice> getInvoicesByLegalDocument(String legalDocument) {
        return invoiceRepository.findByLegalDocument(legalDocument);
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    private Double sumAllProductsAmounts(List<Product> products) {
        return products.stream()
                .mapToDouble(product -> product.getPrice() * product.getQuantity())
                .sum();
    }

}
