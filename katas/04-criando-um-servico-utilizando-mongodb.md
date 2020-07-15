# 04 - Criando um Serviço utilizando MongoDB

## Katas

[00 - Pré-requisitos](/katas/00-pre-requisitos.md)

[01 - Criando um Serviço simples com Spring Boot](/katas/01-criando-um-servico-simples-com-spring-boot.md)

[02 - Empacotando o Serviço dentro de um Container](/katas/02-empacotando-o-servico-dentro-de-um-container.md)

[03 - Criando um Serviço utilizando PostgreSQL](/katas/03-criando-um-servico-utilizando-postgresql.md)

**[04 - Criando um Serviço utilizando MongoDB](/katas/04-criando-um-servico-utilizando-mongodb.md)**

[05 - Orquestrando os Serviços utilizando Docker Compose](/katas/05-orquestrando-os-servicos-utilizando-docker-compose.md)

## Introdução

*Certifique-se de [preencher os pré-requisitos](00-pre-requisitos.md), bem como ter instalado e configurado os programas presentes dessa seção para realizar a atividade.*

Neste exercício, iremos criar um serviço para realizar o gerenciar notas fiscais, que serão persistidos no MongoDB. O serviço será empacotando numa imagem e container do Docker, que irá se conectar ao MariaDB rodando paralelamente em outro container.

*Para essa atividade, você pode [acessar aqui](/invoice-base) o conteúdo base para poder seguir com a explicação. Apenas lembre de renomar o nome da pasta para invoice.*

## Preparando o invoice-service

Para começar essa atividade, você pode criar um novo serviço com o nome `invoice-service` a partir do Spring Initializr, ou pode pegar esse serviço já criado neste link.

Depois que você tiver o serviço no seu computador, entre na pasta do mesmo com o seu Terminal.

```sh
$ cd /caminho/para/o/invoice-service
```

## Subindo o container do MongoDB 4

Para rodarmos o MongoDB 4 dentro de um container Docker, execute o seguinte comando:

```sh
$ docker run --name mongo -d -p 27017:27017 mongo:4
```

Vamos ver se o container subiu corretamente? Execute o comando a seguir:

```sh
$ docker logs mongo
...

2020-07-08T22:08:44.025+0000 I  SHARDING [initandlisten] Marking collection admin.system.roles as collection version: <unsharded>
2020-07-08T22:08:44.025+0000 I  SHARDING [initandlisten] Marking collection admin.system.version as collection version: <unsharded>
2020-07-08T22:08:44.027+0000 I  SHARDING [initandlisten] Marking collection local.startup_log as collection version: <unsharded>
2020-07-08T22:08:44.027+0000 I  FTDC     [initandlisten] Initializing full-time diagnostic data capture with directory '/data/db/diagnostic.data'
2020-07-08T22:08:44.029+0000 I  SHARDING [LogicalSessionCacheRefresh] Marking collection config.system.sessions as collection version: <unsharded>
2020-07-08T22:08:44.029+0000 I  SHARDING [LogicalSessionCacheReap] Marking collection config.transactions as collection version: <unsharded>
2020-07-08T22:08:44.029+0000 I  NETWORK  [listener] Listening on /tmp/mongodb-27017.sock
2020-07-08T22:08:44.030+0000 I  NETWORK  [listener] Listening on 0.0.0.0
2020-07-08T22:08:44.030+0000 I  NETWORK  [listener] waiting for connections on port 27017
2020-07-08T22:08:45.001+0000 I  SHARDING [ftdc] Marking collection local.oplog.rs as collection version: <unsharded>
```

Temos o Mongo configurado e rodando num container na nossa máquina.

## Configurando MongoDB no serviço

Para adicionar o MongoDB no serviço, e com isso facilitar a nossa vida ao lidar com os documentos no na coleção do banco, basta adicionar essa linha abaixo da linha `implementation 'org.springframework.boot:spring-boot-starter-web'`, dentro do arquivo *build.gradle*:

*invoice/build.gradle*

```groovy
...
  
implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'

...
```

Depois, adicione essas configurações ao final do arquivo `application.yml`:

*invoice/src/main/resources/application.yml*

```yaml
...

spring:
  data:
    mongodb:
      host: 'mongo'
      port: '27017'
```

## Criando a estrutura do serviço

Depois de adicionar o MongoDB no nosso projeto, é hora de começar criar a estrutura para o nosso serviço. 

Neste exercício, iremos criar um serviço para gerenciar notas fiscais, realizando a emissão e visualização das notas geradas. Ao emitir as notas, será necessário informar a lista de produtos com nome, quantidade e preço.

Para isso, iremos fazer os seguintes passos: criar o documento para o produto; criar o documento para a nota associado com uma lista de produtos; criar o repositório para acessar o documento da nota;  criar o serviço que emitirá a nota;  e por fim criaremos o controller para expor essas funcionalidades.

### Criando o documento Product

Primero, crie um pacote chamado `document` dentro do caminho `invoice/src/main/java/org/dojo/invoice`. Depois, crie dentro desse pacote a classe `Product`, adicionando o seguinte conteúdo:

*invoice/src/main/java/org/dojo/invoice/document/Product.java*

```java
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

```

### Criando o documento Invoice

Depois de criar o documento `Product`, crie outra classe nesse mesmo pacote chamado `Invoice`, adicionando o seguinte conteúdo:

*invoice/src/main/java/org/dojo/invoice/document/Invoice.java*

```java
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

```

### Criando o repositório para acessar o documento Invoice

Depois de criar o documento `Invoice`, crie um pacote chamado `repository` dentro do caminho `invoice/src/main/java/org/dojo/invoice`. Então, crie dentro desse pacote a interface `InvoiceRepository`, adicionando o seguinte conteúdo:

*invoice/src/main/java/org/dojo/invoice/repository/InvoiceRepository.java*

```java
package org.dojo.invoice.repository;

import org.dojo.invoice.document.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {

    Invoice findByLegalDocument(String legalDocument);

}

```

### Criando o serviço para realizar a emissão e visualização de Invoice

Depois de criar estrutura de documentos para emitir o `Invoice`,  crie um pacote chamado `service` dentro do caminho `invoice/src/main/java/org/dojo/invoice`. A seguir, crie dentro desse pacote a classe `InvoiceService`, adicionando o seguinte conteúdo:

*invoice/src/main/java/org/dojo/invoive/service/InvoiceService.java*

```java
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

```

### Criando o controlador para expor as operações de faturamento

Por fim, crie um pacote chamado `controller` dentro do caminho `invoice/src/main/java/org/dojo/invoice`. A seguir, crie dentro desse pacote a classe `InvoiceController`, adicionando o seguinte conteúdo:

*invoice/src/main/java/org/dojo/invoive/controller/InvoiceController.java*

```java
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

```

## Testando o invoice-service

Depois de seguir esses passo, chegou a hora para vermos o serviço funcionando.

Antes de gerar uma imagem do serviço, precisamos buildar o serviço. Rode o comando abaixo:

```sh
$ ./gradlew build

BUILD SUCCESSFUL in 6s
5 actionable tasks: 5 executed

```

Agora rode o comando para gerar a imagem do nosso serviço:

```shell
$ docker build -t dojo/invoice:0.0.1 .
Sending build context to Docker daemon  44.44MB
Step 1/5 : FROM openjdk:11-jre-slim
 ---> 030d68516e3a
Step 2/5 : MAINTAINER William Brendaw <will@williambrendaw.com>
 ---> Using cache
 ---> cd58ee7be467
Step 3/5 : WORKDIR /app
 ---> Using cache
 ---> 444dd4a1b320
Step 4/5 : COPY build/libs/invoice-0.0.1.jar /app/
 ---> 1403158367b6
Step 5/5 : CMD java -jar invoice-0.0.1.jar
 ---> Running in fbd33bce2839
Removing intermediate container fbd33bce2839
 ---> e84000700104
Successfully built e84000700104
Successfully tagged dojo/invoice:0.0.1
```

Por fim, vamos iniciar um container a partir dessa imagem:

```shell
$ docker run -d --name=invoice -p 8300:8300 --link mongo:mongo dojo/invoice:0.0.1
77a8ed4b50608c18237f22a1c9b575d99b668b152e728cedae1ee02b251fdd3a
```

Agora podemos testar fazendo a emissão de uma nota dentro do serviço:

```shell
$ curl --request POST --header "Content-Type: application/json" --data '[{ "name": "Bloco de anotações", "quantity": 1, "price": 10 }, { "name": "Caneta esferográfica", "quantity": 3, "price": 2 }]' http://localhost:8300/issue/00011122233

{"id":"5f06684fa7c46653a694ae29","legalDocument":"00011122233","products":[{"id":null,"name":"Bloco de anotações","quantity":1,"price":10.0},{"id":null,"name":"Caneta esferográfica","quantity":3,"price":2.0}],"totalAmount":16.0}
```

Beleza, vamos ver se o serviço está retornando a nota por id:

```shell
$ curl --request GET http://localhost:8300/invoices/5f06684fa7c46653a694ae29

{"id":"5f06684fa7c46653a694ae29","legalDocument":"00011122233","products":[{"id":null,"name":"Bloco de anotações","quantity":1,"price":10.0},{"id":null,"name":"Caneta esferográfica","quantity":3,"price":2.0}],"totalAmount":16.0}
```

Maravilha. Agora vamos gerar uma nova nota para o mesmo CPF:

```shell
$ curl --request POST --header "Content-Type: application/json" --data '[{ "name": "Caderno pautado", "quantity": 2, "price": 15 }, { "name": "Lapiseira 0.9", "quantity": 2, "price": 1 }]' http://localhost:8300/issue/00011122233

{"id":"5f06693da7c46653a694ae2a","legalDocument":"00011122233","products":[{"id":null,"name":"Caderno pautado","quantity":2,"price":15.0},{"id":null,"name":"Lapiseira 0.9","quantity":2,"price":1.0}],"totalAmount":32.0}
```

Vamos validar se para esse cpf vai aparecer as duas notas criadas:

```shell
$ curl --request GET http://localhost:8300/invoices/client/00011122233

[{"id":"5f06684fa7c46653a694ae29","legalDocument":"00011122233","products":[{"id":null,"name":"Bloco de anotações","quantity":1,"price":10.0},{"id":null,"name":"Caneta esferográfica","quantity":3,"price":2.0}],"totalAmount":16.0},{"id":"5f06693da7c46653a694ae2a","legalDocument":"00011122233","products":[{"id":null,"name":"Caderno pautado","quantity":2,"price":15.0},{"id":null,"name":"Lapiseira 0.9","quantity":2,"price":1.0}],"totalAmount":32.0}]
```

Perfeito. Para complementar, vamos criar uma nova nota para um outro cpf:

```shell
$ curl --request POST --header "Content-Type: application/json" --data '[{ "name": "Agenda 2020", "quantity": 1, "price": 15 }, { "name": "Lápis 2b", "quantity": 2, "price": 0.5 }]' http://localhost:8300/issue/44455566677

{"id":"5f066ae4a7c46653a694ae2b","legalDocument":"44455566677","products":[{"id":null,"name":"Agenda 2020","quantity":1,"price":15.0},{"id":null,"name":"Lápis 2b","quantity":2,"price":0.5}],"totalAmount":16.0}
```

Por fim, vamos ver se está aparecendo todas as notas criadas:

```shell
$ curl --request GET http://localhost:8300/invoices

[{"id":"5f06684fa7c46653a694ae29","legalDocument":"00011122233","products":[{"id":null,"name":"Bloco de anotações","quantity":1,"price":10.0},{"id":null,"name":"Caneta esferográfica","quantity":3,"price":2.0}],"totalAmount":16.0},{"id":"5f06693da7c46653a694ae2a","legalDocument":"00011122233","products":[{"id":null,"name":"Caderno pautado","quantity":2,"price":15.0},{"id":null,"name":"Lapiseira 0.9","quantity":2,"price":1.0}],"totalAmount":32.0},{"id":"5f066ae4a7c46653a694ae2b","legalDocument":"44455566677","products":[{"id":null,"name":"Agenda 2020","quantity":1,"price":15.0},{"id":null,"name":"Lápis 2b","quantity":2,"price":0.5}],"totalAmount":16.0}]
```

Isso aí. Todas as notas estão retornando.

*Se quiser, você pode [acessar aqui](/invoice-service) para comparar a sua implementação com a implementação esperada.*

## Para ir além

-   [mongo - Docker Hub Image](https://hub.docker.com/_/mongo)
-   [docker images - Docker Documentation](https://docs.docker.com/engine/reference/commandline/images/)
-   [docker build - Docker Documentation](https://docs.docker.com/engine/reference/commandline/build/)
-   [Spring Cloud - Overview](https://spring.io/projects/spring-cloud)
-   [Spring Data JPA - Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#reference)
-   [Spring Data MongoDB - Reference](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#reference)

### Menu

[[<< Atividade Anterior](03-criando-um-servico-utilizando-postgresql.md)] [[Índice](#katas)] [[README](/README.md)] [[Próxima Atividade >>](05-orquestrando-os-servicos-utilizando-docker-compose.md)]