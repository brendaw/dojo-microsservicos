# 02 - Empacotando o Serviço dentro de um Container

## Katas

[00 - Pré-requisitos](/katas/00-pre-requisitos.md)

[01 - Criando um Serviço simples com Spring Boot](/katas/01-criando-um-servico-simples-com-spring-boot.md)

**[02 - Empacotando o Serviço dentro de um Container](/katas/02-empacotando-o-servico-dentro-de-um-container.md)**

[03 - Criando um Serviço utilizando PostgreSQL](/katas/03-criando-um-servico-utilizando-postgresql.md)

[04 - Criando um Serviço utilizando MongoDB](/katas/04-criando-um-servico-utilizando-mongodb.md)

[05 - Orquestrando os Serviços utilizando Docker Compose](/katas/05-orquestrando-os-servicos-utilizando-docker-compose.md)

## Introdução

*Certifique-se de [preencher os pré-requisitos](00-pre-requisitos.md), bem como ter instalado e configurado os programas presentes dessa seção para realizar a atividade.*

Nesta atividade, vamos aprender a como empacotar um serviço em Spring Boot dentro de um container Docker.

*Você pode pular esse exercício caso se sinta confortável trabalhar com Docker. Apenas lembre-se que as próximas atividades assumem que você tenha um bom conhecimento sobre como gerar imagens e containers Docker a partir de serviços Spring Boot.*

## Configurando a geração da imagem Docker

Para deixar o serviço apto a ser empacotado, criaremos um novo tipo de arquivo: o Dockerfile. Nele, terá todas as informações necessárias para gerar uma imagem docker com a última versão compilada do serviço.

Com esse tipo de configuração, será mais fácil realizar o gerenciamento das versões do serviço, e também não ficaremos dependentes de qual sistema o serviço irá rodar: o Docker cuidará disso para nós.

Crie um arquivo com o nome Dockerfile na raiz do repositório do serviço base criado na atividade anterior, com o seguinte conteúdo:

*simple/Dockerfile*

```dockerfile
FROM openjdk:11-jre-slim

MAINTAINER William Brendaw <will@williambrendaw.com>

WORKDIR /app

COPY build/libs/simple-0.0.1-SNAPSHOT.jar /app/

CMD java -jar simple-0.0.1-SNAPSHOT.jar
```

## Gerando a imagem Docker a partir do serviço

Antes de gerar a image, rode o comando que cria os artefatos do serviço, a fim de garantir que a imagem terá a versão mais atualizada do serviço:

```shell
$ ./gradlew build
Starting a Gradle Daemon (subsequent builds will be faster)

> Task :test
2020-07-05 22:07:21.049  INFO 9888 --- [extShutdownHook] o.s.s.concurrent.ThreadPoolTaskExecutor  : Shutting down ExecutorService 'applicationTaskExecutor'

BUILD SUCCESSFUL in 15s
5 actionable tasks: 3 executed, 2 up-to-date
```

Então é hora de rodar o comando do Docker para gerar a imagem a partir dos artefatos do serviço:

```shell
$ docker build -t dojo/simple:0.0.1 .
Sending build context to Docker daemon  18.99MB
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
 ---> Running in d6095bf9f677
Removing intermediate container d6095bf9f677
 ---> e389a13fa7e9
Step 3/5 : WORKDIR /app
 ---> Running in 61e8e87b56ae
Removing intermediate container 61e8e87b56ae
 ---> 3acb7f96b146
Step 4/5 : COPY build/libs/simple-0.0.1-SNAPSHOT.jar /app/
 ---> 9f6b5df558ae
Step 5/5 : CMD java -jar simple-0.0.1-SNAPSHOT.jar
 ---> Running in 488336c86010
Removing intermediate container 488336c86010
 ---> 3b13e9f398f9
Successfully built 3b13e9f398f9
Successfully tagged dojo/simple:0.0.1
```

Pronto. Este comando criou uma imagem Docker com a tag dojo/simple:0.0.1. Agora vamos ver a imagem gerada no repositório local do Docker:

```shell
$ docker images
REPOSITORY          TAG                 IMAGE ID            CREATED              SIZE
dojo/simple         0.0.1               3b13e9f398f9        About a minute ago   223MB
openjdk             11-jre-slim         030d68516e3a        3 weeks ago          204MB
```

E está lá.

## Rodando o container a partir da imagem Docker gerada

Chegou o momento em que vamos rodar o container com a imagem gerada a partir do nosso serviço. Para isso, rode o seguinte comando:

```shell
$ docker run -d --name=simple -p 8100:8100 dojo/simple:0.0.1
```

Vamos conferir se o container está rodando:

```shell
$ docker ps
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                    NAMES
8814b7b21508        dojo/simple:0.0.1   "/bin/sh -c 'java -j…"   25 seconds ago      Up 24 seconds       0.0.0.0:8100->8100/tcp   simple
```

Podemos ver o que está acontecendo dentro do container ao acessar os logs dele:

```shell
$ docker logs simple

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.3.1.RELEASE)

2020-07-06 01:20:38.097  INFO 7 --- [           main] o.dojo.simple.SimpleServiceApplication   : Starting SimpleServiceApplication on 8814b7b21508 with PID 7 (/app/simple-0.0.1-SNAPSHOT.jar started by root in /app)
2020-07-06 01:20:38.100 DEBUG 7 --- [           main] o.dojo.simple.SimpleServiceApplication   : Running with Spring Boot v2.3.1.RELEASE, Spring v5.2.7.RELEASE
2020-07-06 01:20:38.101  INFO 7 --- [           main] o.dojo.simple.SimpleServiceApplication   : No active profile set, falling back to default profiles: default
2020-07-06 01:20:39.940  INFO 7 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8100 (http)
2020-07-06 01:20:39.954  INFO 7 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2020-07-06 01:20:39.955  INFO 7 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.36]
2020-07-06 01:20:40.053  INFO 7 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2020-07-06 01:20:40.053  INFO 7 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 1880 ms
2020-07-06 01:20:40.463  INFO 7 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2020-07-06 01:20:40.768  INFO 7 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoint(s) beneath base path '/actuator'
2020-07-06 01:20:40.814  INFO 7 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8100 (http) with context path ''
2020-07-06 01:20:40.830  INFO 7 --- [           main] o.dojo.simple.SimpleServiceApplication   : Started SimpleServiceApplication in 3.61 seconds (JVM running for 4.167)
```

Beleza, ele rodou sem problemas.

Agora é hora de testar os endpoints do container para ver se está respondendo corretamente. Para isso, basta rodar os seguintes comandos curl para testar o retorno do endpoint /hello e /actuator/health, como abaixo:

```shell
$ curl http://localhost:8100/hello
Hello World!
```

```shell
$ curl http://localhost:8100/actuator/health
{"status":"UP"}
```

E assim temos o nosso serviço empacotado dentro de um container Docker.

## Removendo a imagem e o container Docker

Para limpar a bagunça, primeiro precisamos parar o container Docker:

```shell
$ docker stop simple
simple
```

Depois, precisamos excluir o container do sistema:

```shell
$ docker rm simple
simple
```

Vamos conferir se realmente excluiu:

```shell
$ docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
```

Pronto. Agora é hora de remover a imagem do repositório local do Docker:

```shell
$ docker rmi -f dojo/simple:0.0.1
Untagged: dojo/simple:0.0.1
Deleted: sha256:3b13e9f398f99dd53c2c81367c6439c960870466ed5afd9fbe83c015ff7c53c2
Deleted: sha256:9f6b5df558ae068df463cd3f61081e5720f25b2327839387c3318fd94ba27c33
Deleted: sha256:d555852342cb73abe5c35676753b92b023b68596c08dc2c751134e2f924740b6
Deleted: sha256:3acb7f96b14679645520a483ba6604de5f7e983d13b24128a9a2b8c2c2666911
Deleted: sha256:d6487a4aeda246989ddeafb5c9ee6bd0d31e8a4f37ecac491499e350fed93fa1
Deleted: sha256:e389a13fa7e9360f947aab225e185020a3373e5ed8581c8a805bb35304e7a3e2
```

Vamos conferir se excluiu a imagem:

```shell
$ docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
openjdk             11-jre-slim         030d68516e3a        3 weeks ago         204MB
```

E assim excluimos todos os rastros da nossa imagem e container Docker do nosso serviço.

## Para ir além

-   [openjdk - Docker Hub Image](https://hub.docker.com/_/openjdk)
-   [docker build - Docker Documentation](https://docs.docker.com/engine/reference/commandline/build/)
-   [docker run - Docker Documentation](https://docs.docker.com/engine/reference/commandline/run/)
-   [docker images - Docker Documentation](https://docs.docker.com/engine/reference/commandline/images/)
-   [docker ps - Docker Documentation](https://docs.docker.com/engine/reference/commandline/ps/)
-   [docker logs - Docker Documentation](https://docs.docker.com/engine/reference/commandline/logs/)
-   [docker stop - Docker Documentation](https://docs.docker.com/engine/reference/commandline/stop/)
-   [docker rm - Docker Documentation](https://docs.docker.com/engine/reference/commandline/rm/)
-   [docker rmi - Docker Documentation](https://docs.docker.com/engine/reference/commandline/rmi/)

### Menu

[[<< Atividade Anterior](01-criando-um-servico-simples-com-spring-boot.md)] [[Índice](#katas)] [[README](/README.md)] [[Próxima Atividade >>](03-criando-um-servico-utilizando-postgresql.md)]

