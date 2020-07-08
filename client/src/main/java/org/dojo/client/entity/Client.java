package org.dojo.client.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
public class Client {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String legalDocument;

}
