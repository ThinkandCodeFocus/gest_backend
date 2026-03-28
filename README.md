# ERP Gestion de Flotte - Backend Spring Boot

Ce dépôt contient une base backend Spring Boot pour le projet décrit dans `CC.pdf`.

## Modules couverts dans cette première version

- authentification JWT et rôles
- gestion multi-entreprise
- véhicules
- chauffeurs
- clients
- recettes journalières
- maintenances
- dettes
- tableau de bord synthétique

## Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- H2 en local
- PostgreSQL prêt pour la prod

## Lancer le projet

Vous pouvez lancer le projet avec Maven.

```bash
mvn spring-boot:run
```

Une structure type wrapper Maven est aussi présente :

- `.mvn/wrapper/maven-wrapper.properties`
- `mvnw`
- `mvnw.cmd`

L'API démarre sur `http://localhost:8080/api`.

## Comptes de démonstration

Un administrateur est injecté au démarrage :

- email : `admin@demo.local`
- mot de passe : `admin123`

## Endpoints de départ

- `POST /auth/login`
- `GET /vehicles`
- `POST /vehicles`
- `GET /drivers`
- `POST /drivers`
- `GET /clients`
- `POST /clients`
- `GET /revenues`
- `POST /revenues`
- `GET /maintenances`
- `POST /maintenances`
- `GET /debts`
- `GET /dashboard`

## Structure du projet

- `src/main/java/com/thinkcode/transportbackend/controller`
- `src/main/java/com/thinkcode/transportbackend/dto`
- `src/main/java/com/thinkcode/transportbackend/entity`
- `src/main/java/com/thinkcode/transportbackend/repository`
- `src/main/java/com/thinkcode/transportbackend/service`
- `src/main/java/com/thinkcode/transportbackend/security`
- `src/main/java/com/thinkcode/transportbackend/config`
- `src/test/java/com/thinkcode/transportbackend`

## Règle métier déjà implémentée

Lorsqu'une recette journalière active est inférieure à l'objectif journalier du véhicule, une dette est générée automatiquement.

## Suites recommandées

- ajouter la facturation PDF
- brancher PostgreSQL
- ajouter le chat interne et les notifications
- renforcer l'audit et le versionning documentaire
