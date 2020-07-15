# 05 - Orquestrando os Serviços utilizando Docker Compose

## Katas

[00 - Pré-requisitos](/katas/00-pre-requisitos.md)

[01 - Criando um Serviço simples com Spring Boot](/katas/01-criando-um-servico-simples-com-spring-boot.md)

[02 - Empacotando o Serviço dentro de um Container](/katas/02-empacotando-o-servico-dentro-de-um-container.md)

[03 - Criando um Serviço utilizando PostgreSQL](/katas/03-criando-um-servico-utilizando-postgresql.md)

[04 - Criando um Serviço utilizando MongoDB](/katas/04-criando-um-servico-utilizando-mongodb.md)

**[05 - Orquestrando os Serviços utilizando Docker Compose](/katas/05-orquestrando-os-servicos-utilizando-docker-compose.md)**

## Introdução

*Certifique-se de [preencher os pré-requisitos](00-pre-requisitos.md), bem como ter instalado e configurado os programas presentes dessa seção para realizar a atividade.*

Neste exercício, iremos realizar a orquestração dos serviços e banco de dados configurados até agora. Para isso, faremos a configuração das imagens e containers dentro do Docker Compose, além de modificar os dois serviços para aceitarem configurações externas do orquestrados, bem como modificar o invoice-service para se conectar ao cliente-service na hora de gerar a nota fiscal.

*Certifique-se de ter os serviços desenvolvidos nos dois últimos exercícios para poder seguir com esse treinamento. [Acesse aqui](/client-service) o cliente-service. [E aqui](/invoice-service) o invoice-service.*

## Configurando o docker-compose.yml

Para realizar a orquestração dos serviços, precisamos configurar o Docker Compose. Ele já vem junto ao Docker Desktop ou Docker Machine, dependendo do seu sistema operacional. Para realizar a configuração dele de acordo com as nossas necessidades, crie um arquivo chamado `docker-compose.yml` na mesma pasta em que os outros serviços se encontram, com o seguinte conteúdo:

*docker-compose.yml*

```yaml
version: '3.7'

services:
  client-server:
    build:
      context: client
      dockerfile: Dockerfile
    ports:
      - "8200:8200"
    restart: always
    environment:
      DATASOURCE_URL: jdbc:postgresql://postgres:5432/client
      DATASOURCE_USERNAME: client
      DATASOURCE_PASSWORD: supersecretpassword
    depends_on:
      - postgres   
    networks:
      - postgres_network
      - services_network

  invoice-server:
    build:
      context: invoice
      dockerfile: Dockerfile
    ports:
      - "8300:8300"
    restart: always
    environment:
      CLIENT_SERVICE_URL: http://client-server:8200
      MONGODB_HOST: mongodb
      MONGODB_PORT: '27017'
    depends_on:
      - mongodb
    networks:
      - mongodb_network
      - services_network

  postgres:
    image: postgres:11-alpine
    ports:
      - "5432:5432"
    restart: always
    environment:
      POSTGRES_USER: client
      POSTGRES_DB: client
      POSTGRES_PASSWORD: supersecretpassword
    networks:
      - postgres_network

  mongodb:
    image: mongo:4
    ports:
      - "3306:3306"
    restart: always
    networks:
      - mongodb_network  

networks:
  postgres_network:
  mongodb_network: 
  services_network:
```

A pasta deve ficar com o seguinte conteúdo:

```shell
$ ls -l1
client
docker-compose.yml
invoice
```

## Alterando o client-service para aceitar configurações externas

Após criar o `docker-compose.yml`, vamos alterar o `application.yml` do client-service para receber configurações externas feitas pelo orquestrador.

Para isso, altere a parte da configuração do PostgreSQL para o conteúdo abaixo:

*client/src/main/resources/application.yml*

```yml
spring:
  datasource:
    password: ${DATASOURCE_PASSWORD:supersecretpassword}
    url: ${DATASOURCE_URL:jdbc:postgresql://postgres:5432/client}
    username: ${DATASOURCE_USERNAME:client}
```

## Alterando o invoice-service para aceitar configuraçõers externas

Assim como no client-service, precisamos alterar o `application.yml` do invoque-service para receber configurações externas feitas pelo orquestrador.

Modifique a parte da configuração do MongoDB para o trecho abaixo:

```yaml
spring:
  client-service:
    url: ${CLIENT_SERVICE_URL:http://localhost:8200}
  data:
    mongodb:
      host: ${MONGODB_HOST:mongo}
      port: ${MONGODB_PORT:27017}
```

## Modificando o client-service para retornar o cliente pelo seu legalDocument

Como o invoice-service recebe apenas o parâmetro legalDocument ao gerar as notas fiscais, precisamos alterar o client-service para retornar clientes pelo legalDocument para que o invoice-service possa acessar as informações dos clientes.

### Adicionando o método findByLegalDocument no ClientRepository

Para começar a alteração, precisamos adicionar o método `findByLegalDocument()` no ClienteRepository. Altere o repository, deixando o conteúdo da interface da seguinte forma:

*client/src/main/java/org/dojo/client/repository/ClientRepository.java*

```java
package org.dojo.client.repository;

import org.dojo.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Client findByLegalDocument(String legalDocument);

}

```

### Adicionando o endpoint do legalDocument no ClientController

Depoi de alterar o ClientRepository, é hora de alterar o ClientController para retornar os clientes pelo seu legalDocument cadastrado. Altere o controller para ficar com o seguinte conteúdo:

*client/src/main/java/org/dojo/client/controller/ClientController.java*

```java
package org.dojo.client.controller;

import org.dojo.client.entity.Client;
import org.dojo.client.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("clients")
public class ClientController {

    @Autowired
    private ClientRepository clientRepository;

    @GetMapping
    public List<Client> viewAllClients() {
        return clientRepository.findAll();
    }

    @PostMapping
    public Client createClient(@RequestBody Client client) {
        return clientRepository.saveAndFlush(client);
    }

    @GetMapping("/{id}")
    public Client viewClient(@PathVariable Long id) {
        return clientRepository.findById(id).get();
    }

    @GetMapping("/legal-document/{legalDocument}")
    public Client getClientByLegalDocument(@PathVariable String legalDocument) {
        return Optional.ofNullable(clientRepository.findByLegalDocument(legalDocument))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public Client updateClient(@PathVariable Long id, @RequestBody Client client) {
        client.setId(id);

        return clientRepository.saveAndFlush(client);
    }

    @DeleteMapping("/{id}")
    public void deleteClient(@PathVariable Long id) {
        clientRepository.deleteById(id);
    }

}

```

## Conectando o client-service dentro do invoice-service

Agora, iremos modificar o invoice-service para começar a consumir o cadastro de clientes do client-service e validar se o cliente existe ou não no nosso sistema distribuído. Caso o cliente não exista, então não emitiremos a nota.

### Adicionando a dependência do OpenFeign

Para começar essa configuração, vamos adicionar o OpenFeign como dependência no nosso projeto invoice-service. Adicione as seguintes linhas no `build.gradle`:

*invoice/build.gradle*

```groovy
# Dentro de dependencies
	implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'

# No final do arquivo
dependencyManagement {
	imports {
		mavenBom "org.springframework.cloud:spring-cloud-dependencies:Hoxton.SR5"
	}
}
```

Agora, precisamos adicionar a anotação `@EnableFeignClients` dentro do `InvoiceServiceApplication`. O arquivo ficará assim:

*invoice/src/main/java/org/dojo/invoice/InvoiceServiceApplication.java*

```java
package org.dojo.invoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class InvoiceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvoiceServiceApplication.class, args);
	}

}

```

### Importando a entidade Client no invoice-service como um DTO

Depois da configuração do OpenFeign, precisamos importar a entidade Client que agora será também tratada pelo invoice -service, só que da forma de um DTO. Crie o pacote chamado `dto` dentro do caminho `invoice/src/main/java/org/dojo/invoice`. Então, adicione a classe `ClientDto` dentro desse pacote, com o conteúdo a seguir:

*invoice/src/main/java/org/dojo/invoice/dto/ClientDto.java*

```java
package org.dojo.invoice.dto;

import lombok.Data;

@Data
public class ClientDto {

    private Long id;
    private String name;
    private String legalDocument;

}

```

### Criando a interface OpenFeign para o client-service

Após ter importado a entidade Client como o DTO, é hora de adicionar a interface que será o nosso cliente para se conectar ao client-service. Crie um novo pacote chamado `client` dentro do caminho `invoice/src/main/java/org/dojo/invoice`. A seguir, crie a interface `ClientServiceClient` neste pacote, com o seguinte conteúdo:

*invoice/src/main/java/org/dojo/invoice/client/ClientServiceClient.java*

```java
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

```

### Alterando o documento Invoice para também salvar o nome do cliente

Como teremos a informação completa do cliente fornecida pelo cliente-service, vamos alterar o documento Invoice para além de salvar o legalDocument, também salvar o nome do cliente na nota fiscal. Para isso, basta adicionar o campo `clientName` no `Invoice`,  que ficará com o conteúdo abaixo:

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
    private String clientName;

    private List<Product> products;

    private Double totalAmount;

}
```

### Alterando o InvoiceService para consumir os dados do client-service

Por fim, vamos alterar o InvoiceService para consumir os dados do cliente contidos no client-service quando for gerar a nota fiscal. Caso o legalDocument não exista, nós não emitiremos a nota com o retorno de "cliente não cadastrado". Bora pro código:

*invoice/src/main/java/org/dojo/invoice/service/InvoiceService.java*

```java
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cliente com legalDocument " + legalDocument + " não encontrado no sistema.");
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sistema indisponível no momento.");
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

```



## Buildando e orquestrando os serviços e bancos de dados

Até que enfim chegamos a validação principal desse dojo, que é ver os nossos serviços funcionando entre si orquestrados como os microsserviços que são.

Para isso, vamos buildar os serviços e também gerar as imagens dos mesmos com o docker composer.

Entre em cada pasta dos serviços e rode o comando `./gradlew build`, como abaixo:

```shell
$ ls -l1
client
docker-compose.yml
invoice

$ cd client

$ ./gradlew build

BUILD SUCCESSFUL in 2s
5 actionable tasks: 5 up-to-date

$ cd ..

$ cd invoice

$ ./gradlew build

BUILD SUCCESSFUL in 5s
5 actionable tasks: 5 executed

$ cd ..

$
```

Agora podemos pedir para o Docker Compose gerar as imagens dos serviços e dos banco de dados. Rode o seguinte comando:

```shell
$ docker-compose build
postgres uses an image, skipping
mongodb uses an image, skipping
Building client-server
Step 1/5 : FROM openjdk:11-jre-slim
11-jre-slim: Pulling from library/openjdk
8559a31e96f4: Pull complete
65306eca6b8e: Pull complete
ddbf88050b6e: Pull complete
0cb03c61bf26: Pull complete
Digest: sha256:afd1c9c9138dc4bfe062f15ce74b2d0d9518d8f6f8309881e8f821cb5b182bf0
Status: Downloaded newer image for openjdk:11-jre-slim
 ---> 030d68516e3a
Step 2/5 : MAINTAINER William Brendaw <will@williambrendaw.com>
 ---> Running in 2d81f01d6ac9
Removing intermediate container 2d81f01d6ac9
 ---> ada01087764e
Step 3/5 : WORKDIR /app
 ---> Running in b8978fb72476
Removing intermediate container b8978fb72476
 ---> 56c289a11973
Step 4/5 : COPY build/libs/client-0.0.1.jar /app/
 ---> d0cc501156e0
Step 5/5 : CMD java -jar client-0.0.1.jar
 ---> Running in e8ab169d9147
Removing intermediate container e8ab169d9147
 ---> 2afcae028513

Successfully built 2afcae028513
Successfully tagged dojo-microsservicos_client-server:latest
Building invoice-server
Step 1/5 : FROM openjdk:11-jre-slim
 ---> 030d68516e3a
Step 2/5 : MAINTAINER William Brendaw <will@williambrendaw.com>
 ---> Using cache
 ---> ada01087764e
Step 3/5 : WORKDIR /app
 ---> Using cache
 ---> 56c289a11973
Step 4/5 : COPY build/libs/invoice-0.0.1.jar /app/
 ---> 47381a4fb67b
Step 5/5 : CMD java -jar invoice-0.0.1.jar
 ---> Running in d789ec788124
Removing intermediate container d789ec788124
 ---> a945edf53a49

Successfully built a945edf53a49
Successfully tagged dojo-microsservicos_invoice-server:latest

$
```

E então chegou o momento para orquestrar todo mundo pelo docker compose. Rode o comando abaixo:

```shell
$ docker-compose up
Pulling postgres (postgres:11-alpine)...
11-alpine: Pulling from library/postgres
df20fa9351a1: Pull complete
600cd4e17445: Pull complete
04c8eedc9a76: Pull complete
2704c561b4e3: Pull complete
db082802a6cb: Pull complete
4d2a93e8110b: Pull complete
ae8ba07f5a37: Pull complete
3cfbb9fe5e84: Pull complete
005b6bf61c53: Pull complete
Digest: sha256:fed26d3b33ed6c29437d1afaf483a60bf39c10f878e7ae5662436862e743e3b0
Status: Downloaded newer image for postgres:11-alpine
Pulling mongodb (mongo:4)...
4: Pulling from library/mongo
a1125296b23d: Pull complete
3c742a4a0f38: Pull complete
4c5ea3b32996: Pull complete
1b4be91ead68: Pull complete
af8504826779: Pull complete
8faaabd5f8b2: Pull complete
b7bb90b3c1e5: Pull complete
04f4b579cf84: Pull complete
33b916e924f1: Pull complete
a805b21b6014: Pull complete
7775fed63862: Pull complete
94226c388074: Pull complete
bdf7bf6a32e9: Pull complete
Digest: sha256:4ab515e5e8ecab779517062daf607001cbba750699e7338f01e473ee68e2e098
Status: Downloaded newer image for mongo:4
Creating dojo-microsservicos_mongodb_1  ... done
Creating dojo-microsservicos_postgres_1       ... done
Creating dojo-microsservicos_invoice-server_1 ... done
Creating dojo-microsservicos_client-server_1  ... done

...

invoice-server_1  | 2020-07-15 00:14:39.855  WARN 6 --- [           main] o.s.c.n.a.ArchaiusAutoConfiguration      : No spring.application.name found, defaulting to 'application'
invoice-server_1  | 2020-07-15 00:14:39.867  WARN 6 --- [           main] c.n.c.sources.URLConfigurationSource     : No URLs will be polled as dynamic configuration sources.
invoice-server_1  | 2020-07-15 00:14:39.867  INFO 6 --- [           main] c.n.c.sources.URLConfigurationSource     : To enable URLs as dynamic configuration sources, define System property archaius.configurationSource.additionalUrls or make config.properties available on classpath.
invoice-server_1  | 2020-07-15 00:14:39.888  WARN 6 --- [           main] c.n.c.sources.URLConfigurationSource     : No URLs will be polled as dynamic configuration sources.
invoice-server_1  | 2020-07-15 00:14:39.889  INFO 6 --- [           main] c.n.c.sources.URLConfigurationSource     : To enable URLs as dynamic configuration sources, define System property archaius.configurationSource.additionalUrls or make config.properties available on classpath.
client-server_1   | 2020-07-15 00:14:39.967  INFO 7 --- [         task-1] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
client-server_1   | 2020-07-15 00:14:40.324  INFO 7 --- [         task-1] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
invoice-server_1  | 2020-07-15 00:14:40.327  INFO 6 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
client-server_1   | 2020-07-15 00:14:40.410  INFO 7 --- [         task-1] org.hibernate.dialect.Dialect            : HHH000400: Using dialect: org.hibernate.dialect.PostgreSQLDialect
invoice-server_1  | 2020-07-15 00:14:41.198  INFO 6 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoint(s) beneath base path '/actuator'
invoice-server_1  | 2020-07-15 00:14:41.378  INFO 6 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8300 (http) with context path ''
invoice-server_1  | 2020-07-15 00:14:41.445  INFO 6 --- [           main] o.d.invoice.InvoiceServiceApplication    : Started InvoiceServiceApplication in 11.393 seconds (JVM running for 13.53)
client-server_1   | 2020-07-15 00:14:41.942  INFO 7 --- [         task-1] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000490: Using JtaPlatform implementation: [org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform]
client-server_1   | 2020-07-15 00:14:41.951  INFO 7 --- [         task-1] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
client-server_1   | 2020-07-15 00:14:42.052  INFO 7 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoint(s) beneath base path '/actuator'
client-server_1   | 2020-07-15 00:14:42.126  INFO 7 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8200 (http) with context path ''
client-server_1   | 2020-07-15 00:14:42.128  INFO 7 --- [           main] DeferredRepositoryInitializationListener : Triggering deferred initialization of Spring Data repositories…
client-server_1   | 2020-07-15 00:14:42.599  INFO 7 --- [           main] DeferredRepositoryInitializationListener : Spring Data repositories initialized!
client-server_1   | 2020-07-15 00:14:42.626  INFO 7 --- [           main] o.dojo.client.ClientServiceApplication   : Started ClientServiceApplication in 12.86 seconds (JVM running for 14.722)
```

Agora sim temos os nossos microsserviços rodando por meio de um orquestrador!

## Testando a conexão dos nossos serviços

Vamos criar dois usuários dentro do client-service que está sendo orquestrado pelo Docker Compose. Rode o comando a seguir:

```shell
$ curl --request POST --header "Content-Type: application/json" --data '{"name":"William","legalDocument":"11122233344"}' http://localhost:8200/clients

{"id":1,"name":"William","legalDocument":"11122233344"}

$ curl --request POST --header "Content-Type: application/json" --data '{"name":"Brendaw","legalDocument":"55566677788"}' http://localhost:8200/clients

{"id":2,"name":"Brendaw","legalDocument":"55566677788"}

```

Beleza. Agora vamos tentar gerar uma nota fiscal com um legalDocument inválido. Rode o seguinte comando:

```sh
$ curl --request POST --header "Content-Type: application/json" --data '[{ "name": "Agenda 2020", "quantity": 1, "price": 15 }, { "name": "Lápis 2b", "quantity": 2, "price": 0.5 }]' http://localhost:8300/issue/000000000

{"timestamp":"2020-07-15T21:31:47.566+00:00","status":400,"error":"Bad Request","message":"","path":"/issue/000000000"}
```

Validou e retornou Bad Request. Vamos tentar agora com um legalDocument válido:

```shell
$ curl --request POST --header "Content-Type: application/json" --data '[{ "name": "Agenda 2020", "quantity": 1, "price": 15 }, { "name": "Lápis 2b", "quantity": 2, "price": 0.5 }]' http://localhost:8300/issue/11122233344

{"id":"5f0e62ec41555d11d0bc3e9a","legalDocument":"11122233344","clientName":"William","products":[{"id":null,"name":"Agenda 2020","quantity":1,"price":15.0},{"id":null,"name":"Lápis 2b","quantity":2,"price":0.5}],"totalAmount":16.0}
```

Maravilha. Vamos adicionar outra nota, utilizando o outro legalDocument cadastrado:

```shell
$ curl --request POST --header "Content-Type: application/json" --data '[{"id":null,"name":"Caderno pautado","quantity":2,"price":15.0},{"id":null,"name":"Lapiseira 0.9","quantity":2,"price":1.0}]' http://localhost:8300/issue/55566677788

{"id":"5f0e630941555d11d0bc3e9b","legalDocument":"55566677788","clientName":"Brendaw","products":[{"id":null,"name":"Caderno pautado","quantity":2,"price":15.0},{"id":null,"name":"Lapiseira 0.9","quantity":2,"price":1.0}],"totalAmount":32.0}
```

Vamos ver todas as notas retornadas pelo serviço. Execute o comando abaixo:

```shell
$ curl --request GET http://localhost:8300/invoices

[{"id":"5f0e62ec41555d11d0bc3e9a","legalDocument":"11122233344","clientName":"William","products":[{"id":null,"name":"Agenda 2020","quantity":1,"price":15.0},{"id":null,"name":"Lápis 2b","quantity":2,"price":0.5}],"totalAmount":16.0},{"id":"5f0e630941555d11d0bc3e9b","legalDocument":"55566677788","clientName":"Brendaw","products":[{"id":null,"name":"Caderno pautado","quantity":2,"price":15.0},{"id":null,"name":"Lapiseira 0.9","quantity":2,"price":1.0}],"totalAmount":32.0}]
```

Assim, enfim, temos nossos microsserviços orquestrados e comunicando entre si.

*Se quiser, você pode [acessar aqui](/invoice) o invoice-service, [e aqui](/client) o client service, para comparar a sua implementação com a implementação esperada.*

## Para ir além

-   [Compose file version 3 reference - Docker Documentation](https://docs.docker.com/compose/compose-file/)
-   [Externalized Configuration - Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/2.1.9.RELEASE/reference/html/boot-features-external-config.html)
-   [Query Creation - Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation)
-   [Spring Cloud OpenFeign Documentation](https://cloud.spring.io/spring-cloud-openfeign/reference/html/)
-   [docker-compose build - Docker Documentation](https://docs.docker.com/compose/reference/build/)
-   [docker-compose up - Docker Documentation](https://docs.docker.com/compose/reference/up/)
-   [docker-compose stop - Docker Documentation](https://docs.docker.com/compose/reference/stop/)

### Menu

[[<< Atividade Anterior](04-criando-um-servico-utilizando-mongodb.md)] [[Índice](#katas)] [[README](/README.md)]