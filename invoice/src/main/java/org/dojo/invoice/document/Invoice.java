package org.dojo.invoice.document;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
@Builder
public class Invoice {

    @Id
    private String id;

    private String legalDocument;

    private List<Product> products;

    private Double totalAmount;

}
