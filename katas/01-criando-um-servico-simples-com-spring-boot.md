# 01 - Criando um Serviço simples com Spring Boot

## Katas

[00 - Pré-requisitos](/katas/00-pre-requisitos.md)

**[01 - Criando um Serviço simples com Spring Boot](/katas/01-criando-um-servico-simples-com-spring-boot.md)**

[02 - Empacotando o Serviço dentro de um Container](/katas/02-empacotando-o-servico-dentro-de-um-container.md)

[03 - Criando um Serviço utilizando PostgreSQL](/katas/03-criando-um-servico-utilizando-postgresql.md)

[04 - Criando um Serviço utilizando MongoDB](/katas/04-criando-um-servico-utilizando-mongodb.md)

[05 - Orquestrando os Serviços utilizando Docker Compose](/katas/05-orquestrando-os-servicos-utilizando-docker-compose.md)

## Introdução

*Certifique-se de [preencher os pré-requisitos](00-pre-requisitos.md), bem como ter instalado e configurado os programas presentes dessa seção para realizar a atividade.*

Nesta atividade, iremos criar um serviço REST simples em Spring Boot que retorna "Hello World" a partir do endpoint `/hello`.

*Você pode pular esse exercício caso se sinta confortável em desenvolver aplicações com Spring Boot. Apenas lembre-se que as próximas atividades assumem que você tenha um bom conhecimento sobre os princícios do Spring Boot.*

## Passo-a-passo para criar a base do serviço

1.  Vá para a página do [Spring Initializr](https://start.spring.io/).
2.  Selecione ou preencha os campos com as seguintes opções:
    1.  **Project:** *Gradle Project*
    2.  **Language:** Java
    3.  **Spring Boot:** *2.3.1*
    4.  **Project Metadata:**
        1.  **Group: ** *org.dojo*
        2.  **Artifact:** *simple*
        3.  **Name:** *simple-service*
        4.  **Description:** *Um serviço simples com Spring Boot.*
        5.  **Package name:** *org.dojo.simple*
        6.  **Packaging:** *Jar*
        7.  **Java:** *11*
    5.  **Dependencies:** *Adicione as dependências **Spring Web** e **Spring Boot Actuator***.
    6.  Clique no botão ***Generate***.
3.  Baixe o projeto e descompacte numa pasta de fácil acesso.
4.  Abra o projeto descompactado no seu editor de texto preferido para trabalhar com Java. Caso o seu editor trabalhe com Gradle, importe o mesmo como um projeto Gradle.

## Ajustando algumas configurações

Para começar, vamos mexer na configuração global do nosso serviço. 

Primeiro precisamos renomear o arquivo `simple/src/main/resources/application.properties` para `simple/src/main/resources/application.yml`, a fim de facilitar as nossas configurações. 

Depois, adicionaremos o seguinte conteúdo no nosso novo *application.yml*:

```yaml
server:
  port: 8100
  tomcat:
    max-threads: 200
    max-http-post-size: 2MB
    max-swallow-size: 2MB
    max-http-header-size: 8KB

endpoints:
  health:
    sensitive: false

logging:
  level:
    org.dojo.simple: DEBUG
```

## Criando o nosso Hello World

Essa atividade não seria introdutória de fato se nós não criássemos um Hello Word. 

Para isso, vamos criar um novo pacote (pasta) com o nome `service` dentro de `simple/src/main/java/org/dojo/simple`. 

E dentro desse pacote criaremos a classe `HelloWorldService` com esse conteúdo:

```java
package org.dojo.simple.service;

import org.springframework.stereotype.Service;

@Service
public class HelloWorldService {

    public String getHelloWorld() {
        return "Hello World!";
    }

}
```

Depois disso, precisamos criar um novo pacote com o nome `controller`, que também ficará dentro de `simple/src/main/java/org/dojo/simple`. 

E dentro desse novo pacote criaremos a classe `HelloWorldController`, com o conteúdo seguinte:

```java
package org.dojo.simple.controller;

import org.dojo.simple.service.HelloWorldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    @Autowired
    private HelloWorldService helloWorldService;

    @GetMapping("/hello")
    public ResponseEntity<String> helloWorld() {
        return ResponseEntity.ok(helloWorldService.getHelloWorld());
    }

}
```

## Buildando e executando nosso serviço

No terminal, entre na pasta do projeto e rode do Gradle o comando para gerar os artefatos do serviço:

```shell
$ cd simple

# Para Linux/macOS
$ ./gradlew build

# Para Windows
$ gradlew.bat build
```

Para rodar o serviço gerado, rode o seguinte comando de dentro da mesma pasta:

```shell
# Para Linux/macOS
$ java -jar build/libs/simple-0.0.1-SNAPSHOT.jar

# Para Windows
$ java -jar build\libs\simple-0.0.1-SNAPSHOT.jar
```

Pronto! Seu serviço estará rodando na porta 8100.

## Brincando um pouco com o serviço

### Testando o endpoint de Hello World

Fazendo uma requisição para o endereço `http://localhost:8100/hello` tem que retornar `Hello world!` de acordo com o que implementamos. Rode o comando:

```shell
$ curl http://localhost:8100/hello
Hello World!
```

Você também pode usar o Postman para realizar essa requisição ou abrir esse endereço no navegador que irá retornar essa mensagem.

### Testando a saúde do serviço

Fazendo uma requisição para o endereço `http://localhost:8100/actuator/health` tem que retornar `{"status":"UP"}`. Rode o comando:

```shell
$ curl http://localhost:8100/actuator/health
{"status":"UP"}

```

## Para ir além

-   [Spring Initializr](https://start.spring.io/)
-   [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
-   [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
-   [Gradle Build Tool](https://gradle.org)
-   [Gradle Command-Line Interface](https://docs.gradle.org/current/userguide/command_line_interface.html)

### Menu

[Ver índice de Katas](#katas)

[Ir para a próxima atividade](02-empacotando-o-servico-dentro-de-um-container.md)

[Ir para a atividade anterior](00-pre-requisitos.md)

[Voltar para o README](/README.md)