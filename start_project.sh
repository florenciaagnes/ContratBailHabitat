#!/bin/bash
set -e

echo "=========================================================================="
echo " LEGALTECH MADAGASCAR - AUDIT JURIDIQUE DES BAUX D'HABITATION"
echo "=========================================================================="

# 1. Start Python Flask Audit Service
echo "[1/2] Lancement du Microservice Audit Juridique (Python Flask)..."
cd legal-audit-service
if [ ! -d "venv" ]; then
    python3 -m venv venv
    source venv/bin/activate
    pip install -r requirements.txt
else
    source venv/bin/activate
fi

python3 app.py &
FLASK_PID=$!
cd ..

echo " -> Microservice Flask démarré avec le PID $FLASK_PID sur http://localhost:5000"

# 2. Start Spring Boot Application (PostgreSQL persistence mode)
echo "[2/2] Lancement de l'Application Spring Boot avec PostgreSQL..."
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run


