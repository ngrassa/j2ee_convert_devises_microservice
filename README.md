# Convertisseur de devises en microservices J2ee (deploiment sur un conteneurTomcat 9)

Projet pédagogique J2EE montrant une architecture microservices pour convertir le Dinar Tunisien (TND) vers EUR, USD, GBP, JPY et CAD. Chaque service est packagé en `.war` et déployé sur Tomcat 9, orchestré avec Docker Compose.

## Prérequis (Ubuntu 25.04 testé)
- Docker et le plugin Docker Compose v2 (`docker compose`).
- Accès réseau pour télécharger les images de base.
- (Optionnel) Java 21 + Maven si vous voulez builder sans Docker.
- Script d’aide: `setup.sh` installe Java 21/ Maven/ Docker.

## Services
- **rates-service** (Tomcat 9 + Java 21, port 9081): expose les taux de référence en lecture seule (`/api/rates`, `/api/rates/{code}`).
- **converter-service** (Tomcat 9 + Java 21, port 9082): reçoit un montant en TND et un code cible (`/api/convert?amount=...&to=...`), appelle le service des taux, renvoie le résultat.
- **frontend** (Nginx, port 9080): page HTML/CSS/JS statique, proxy `/api/*` vers le converter.

## Démarrage rapide
```bash
./setup.sh            # build + docker compose up -d
```
Puis ouvrir http://localhost:9080.

## Architecture (pour les étudiants)
- **Isolation**: chaque fonctionnalité est un service Tomcat indépendant, empaqueté en WAR.
- **API REST**: JAX-RS (Jersey) sur `/api/*` pour éviter les collisions de ressources.
- **Chaînage**: le converter appelle le rates-service via HTTP interne Docker (`rates-service:8080/api`).
- **Stateless**: pas de session ni de base de données; tout est recomputable à chaque requête.
- **Réseau**: Docker Compose crée un réseau commun; les noms de service font office de DNS interne.
- **Sécurité simple**: CORS ouvert pour faciliter les appels depuis le frontend de démonstration.

## Points clés de déploiement
- Tomcat 9 + Java 21 dans les conteneurs (images tomcat:9.0-jdk21-temurin).
- Build multi-étapes: Maven construit le WAR, puis Tomcat embarque uniquement l’artifact final.
- Ports exposés: frontend `9080`, converter `9082`, rates `9081`.
- Variable d’env pour le converter: `RATES_SERVICE_URL` (défaut `http://rates-service:8080/api`).

## Accès
- Frontend : http://localhost:9080
- API converter : http://localhost:9082/api/convert?amount=100&from=TND&to=EUR
- API rates : http://localhost:9081/api/rates

## Relancer uniquement un service (exemple)
Depuis la racine du projet :
```bash
cd /home/osboxes/j2ee_convert_devises_microservice
SERVICES=frontend ./setup.sh   # rebuild + restart uniquement le frontend
```
Sans `SERVICES`, le script reconstruit et relance tout le stack.

## Tests rapides
- Taux: `curl http://localhost:9081/api/rates`
- Conversion: `curl http://localhost:9082/api/convert?amount=100&from=EUR&to=USD"`

## Nettoyage
```bash
docker compose down
```
