# 03 - Criando um Serviço utilizando PostgreSQL

## Katas

[00 - Pré-requisitos](/katas/00-pre-requisitos.md)

[01 - Criando um Serviço simples com Spring Boot](/katas/01-criando-um-servico-simples-com-spring-boot.md)

[02 - Empacotando o Serviço dentro de um Container](/katas/02-empacotando-o-servico-dentro-de-um-container.md)

**[03 - Criando um Serviço utilizando PostgreSQL](/katas/03-criando-um-servico-utilizando-postgresql.md)**

[04 - Criando um Serviço utilizando MongoDB](/katas/04-criando-um-servico-utilizando-mongodb.md)

[05 - Orquestrando os Serviços utilizando Docker Compose](/katas/05-orquestrando-os-servicos-utilizando-docker-compose.md)

## Introdução

*Certifique-se de [preencher os pré-requisitos](00-pre-requisitos.md), bem como ter instalado e configurado os programas presentes nessa seção para realizar a atividade.*

Neste exercício, iremos criar um serviço para realizar o gerenciamento dos registros dos clientes, que serão persistidos no PostgreSQL.  O serviço será empacotando numa imagem e container do Docker, que irá se conectar ao PostgreSQL rodando paralelamente em outro container.

*Para essa atividade, você pode [acessar aqui](/client-base) o conteúdo base para poder seguir com a explicação. Apenas lembre de renomar o nome da pasta para client.*

## Preparando o client-service

Para começar essa atividade, você pode criar um novo serviço com o nome `client-service` a partir do Spring Initializr, ou pode pegar esse serviço já criado [neste link](/client-base).

Depois que você tiver o serviço no seu computador, entre na pasta do mesmo com o seu Terminal.

```sh
$ cd /caminho/para/o/client-service
```

## Subindo o container do PostgreSQL 11

Para rodar o PostgreSQL 11 dentro de um container Docker, execute o seguinte comando:

```sh
$ docker run --name postgres -e POSTGRES_USER=client -e POSTGRES_DB=client -e POSTGRES_PASSWORD=supersecretpassword -d -p 5432:5432 postgres:11-alpine
```

Vamos ver se o container subiu corretamente? Execute o comando a seguir:

```shell
$ docker logs postgres
...

PostgreSQL init process complete; ready for start up.

2020-07-06 22:00:17.362 UTC [1] LOG:  listening on IPv4 address "0.0.0.0", port 5432
2020-07-06 22:00:17.362 UTC [1] LOG:  listening on IPv6 address "::", port 5432
2020-07-06 22:00:17.366 UTC [1] LOG:  listening on Unix socket "/var/run/postgresql/.s.PGSQL.5432"
2020-07-06 22:00:17.379 UTC [55] LOG:  database system was shut down at 2020-07-06 22:00:17 UTC
2020-07-06 22:00:17.383 UTC [1] LOG:  database system is ready to accept connections
```

Pronto! Temos o PostgreSQL 11 rodando na nossa máquina dentro de um container Docker.

## Configurando PostgreSQL e JPA no serviço

Para adicionar o PostgreSQL e o Java Persistence API (JPA), e com isso facilitar a nossa vida ao lidar com as entidades no banco, basta adicionar essas duas linhas abaixo da linha `implementation 'org.springframework.boot:spring-boot-starter-web'`, dentro do arquivo *build.gradle*:

*client/build.gradle*

```groovy
...
  
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.postgresql:postgresql'

...
```

Depois, adicione essas configurações ao final do arquivo `application.yml`:

*client/src/main/resources/application.yml*

```yaml
...

spring:
  datasource:
    password: supersecretpassword
    url: jdbc:postgresql://postgres:5432/client
    username: client
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```



## Criando a estrutura do serviço

Depois de adicionar o PostgreSQL e o JPA no nosso projeto, é hora de começar criar a estrutura para o nosso serviço. 

Neste exercício, iremos criar um serviço para gerenciar os registros de clientes, realizando o cadastro, leitura, alteração e remoção dos mesmos. Um CRUD clássico. 

Para isso, iremos fazer os seguintes passos: criar a entidade a ser salva no banco; criar o repositório para acessar a entidade; e por fim criaremos o controller para expor essas funcionalidades.

### Criando a entidade Client

Primero, crie um pacote chamado `entity` dentro do caminho `client/src/main/java/org/dojo/client`. Depois, crie dentro desse pacote a classe `Client`, adicionando o seguinte conteúdo:

*client/src/main/java/org/dojo/client/entity/Client.java*

```java
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
```

### Criando o repositório para acessar a entidade Client

Depois de criar a entidade `Client`, crie um pacote chamado `repository` dentro do caminho `client/src/main/java/org/dojo/client`. Então, crie dentro desse pacote a interface `ClientRepository`, adicionando o seguinte conteúdo:

*client/src/main/java/org/dojo/client/repository/ClientRepository.java*

```java
package org.dojo.client.repository;

import org.dojo.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

}

```

### Criando o controlador para expor as ações sobre a entidade Client

Por fim, crie um pacote chamado `controller` dentro do caminho `client/src/main/java/org/dojo/client`. A seguir, crie dentro desse pacote a classe `ClientController`, adicionando o seguinte conteúdo:

*client/src/main/java/org/dojo/client/controller/ClientController.java*

```java
package org.dojo.client.controller;

import org.dojo.client.entity.Client;
import org.dojo.client.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

## Testando o client-service

Depois de seguir esses passo, chegou a hora para vermos o serviço funcionando.

Antes de gerar uma imagem do serviço, precisamos buildar o serviço. Rode o comando abaixo:

```sh
$ ./gradlew build

BUILD SUCCESSFUL in 4s
5 actionable tasks: 3 executed, 2 up-to-date
```

Agora rode o comando para gerar a imagem do nosso serviço:

```shell
$ docker build -t dojo/client:0.0.1 .
Sending build context to Docker daemon  39.17MB
Step 1/5 : FROM openjdk:11-jre-slim
 ---> 030d68516e3a
Step 2/5 : MAINTAINER William Brendaw <will@williambrendaw.com>
 ---> Running in a2e86a26b8f0
Removing intermediate container a2e86a26b8f0
 ---> c349921af4d5
Step 3/5 : WORKDIR /app
 ---> Running in 59668efcc961
Removing intermediate container 59668efcc961
 ---> 606d8fc60ae1
Step 4/5 : COPY build/libs/client-0.0.1.jar /app/
 ---> 5b12bf915beb
Step 5/5 : CMD java -jar client-0.0.1.jar
 ---> Running in ce70168110f2
Removing intermediate container ce70168110f2
 ---> aa510cd9028e
Successfully built aa510cd9028e
Successfully tagged dojo/simple:0.0.1
```

Por fim, vamos iniciar um container a partir dessa imagem:

```shell
$ docker run -d --name=client -p 8200:8200 --link postgres:postgres dojo/client:0.0.1

```

Agora podemos testar criando um cliente dentro do serviço:

```shell
$ curl --request POST --header "Content-Type: application/json" --data '{"name": "William", "legalDocument": "00000000000"}' http://localhost:8200/clients

{"id":1,"name":"William","legalDocument":"00000000000"}
```

Beleza, vamos ver se ele foi salvo mesmo:

```shell
$ curl --request GET http://localhost:8200/clients/1

{"id":1,"name":"William","legalDocument":"00000000000"}
```

Muito bom! Agora vamos testar a alteração do `legalDocument` do cliente para um novo valor:

```shell
$ curl --request PUT --header "Content-Type: application/json" --data '{"name": "Brendaw", "legalDocument": "11111111111"}' http://localhost:8200/clients/1

{"id":1,"name":"William","legalDocument":"11111111111"}
```

Por fim, vamos ver se essa informação realmente foi alterada:

```shell
$ curl --request GET http://localhost:8200/clients/1

{"id":1,"name":"William","legalDocument":"11111111111"}
```

Perfeito! Temos o nosso serviço fazendo o CRUD da forma como queríamos.

*Se quiser, você pode [acessar aqui](/client-service) para comparar a sua implementação com a implementação esperada.*

## Para ir além

-   [postgres - Docker Hub Image](https://hub.docker.com/_/postgres)
-   [docker images - Docker Documentation](https://docs.docker.com/engine/reference/commandline/images/)
-   [docker build - Docker Documentation](https://docs.docker.com/engine/reference/commandline/build/)
-   [Spring Cloud - Overview](https://spring.io/projects/spring-cloud)
-   [Spring Data JPA - Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#reference)

### Menu

[[<< Atividade Anterior](02-empacotando-o-servico-dentro-de-um-container.md)] [[Índice](#katas)] [[README](/README.md)] [[Próxima Atividade >>](04-criando-um-servico-utilizando-mongodb.md)]

