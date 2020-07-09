package org.dojo.invoice.controller;

import org.dojo.invoice.document.Invoice;
import org.dojo.invoice.document.Product;
import org.dojo.invoice.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @PostMapping("issue/{legalDocument}")
    public Invoice issueInvoice(@PathVariable String legalDocument, @RequestBody List<Product> products) {
        return invoiceService.issueInvoice(legalDocument, products);
    }

    @GetMapping("invoices/{invoiceId}")
    public Invoice getInvoiceById(@PathVariable String invoiceId) {
        return invoiceService.getInvoiceById(invoiceId);
    }

    @GetMapping("invoices/client/{legalDocument}")
    public List<Invoice> getInvoiceByLegalDocument(@PathVariable String legalDocument) {
        return invoiceService.getInvoicesByLegalDocument(legalDocument);
    }

    @GetMapping("invoices")
    public List<Invoice> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }

}
