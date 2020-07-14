package org.dojo.invoice.document;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Product {

    @Id
    private String id;

    private String name;
    private Integer quantity;
    private Double price;

}
