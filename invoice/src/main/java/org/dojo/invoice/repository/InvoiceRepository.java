package org.dojo.invoice.repository;

import org.dojo.invoice.document.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {

    List<Invoice> findByLegalDocument(String legalDocument);

}
