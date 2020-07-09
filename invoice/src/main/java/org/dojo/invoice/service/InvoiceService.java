package org.dojo.invoice.service;

import org.dojo.invoice.document.Invoice;
import org.dojo.invoice.document.Product;
import org.dojo.invoice.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    public Invoice issueInvoice(String legalDocument, List<Product> products) {
        return invoiceRepository.save(Invoice.builder()
                    .legalDocument(legalDocument)
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
