#!/bin/bash

echo "======================================"
echo "Tests API Loto Bingo avec Newman"
echo "======================================"
echo ""

# Vérifier que Newman est installé
if ! command -v newman &> /dev/null
then
    echo "❌ Newman n'est pas installé. Installez-le avec: npm install -g newman"
    exit 1
fi

echo "✅ Newman est installé"
echo ""

# Vérifier que l'application est démarrée
echo "🔍 Vérification que l'application est démarrée..."
if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "⚠️  L'application ne répond pas sur http://localhost:8080"
    echo "   Tentative de démarrage de l'application..."

    # Démarrer l'application en arrière-plan
    ./mvnw spring-boot:run > /dev/null 2>&1 &
    APP_PID=$!

    echo "   En attente du démarrage (max 60s)..."
    for i in {1..60}; do
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo "✅ Application démarrée (PID: $APP_PID)"
            STARTED_BY_SCRIPT=true
            break
        fi
        sleep 1
        echo -n "."
    done

    if [ "$STARTED_BY_SCRIPT" != "true" ]; then
        echo ""
        echo "❌ Impossible de démarrer l'application"
        kill $APP_PID 2>/dev/null
        exit 1
    fi
else
    echo "✅ Application déjà démarrée"
fi
echo ""

# Créer le dossier de rapports
mkdir -p postman/reports

# Exécuter les tests
echo "🚀 Exécution des tests..."
echo ""

TIMESTAMP=$(date +%Y%m%d-%H%M%S)

newman run postman/loto-bingo-api.postman_collection.json \
  -e postman/loto-bingo-environment.postman_environment.json \
  -r cli,json \
  --reporter-json-export "postman/reports/report-$TIMESTAMP.json" \
  --delay-request 200 \
  --color on \
  --bail

TEST_RESULT=$?

# Vérifier si htmlextra est installé et l'utiliser
if command -v newman &> /dev/null && newman run --help | grep -q "htmlextra"; then
    echo ""
    echo "📊 Génération du rapport HTML..."
    newman run postman/loto-bingo-api.postman_collection.json \
      -e postman/loto-bingo-environment.postman_environment.json \
      -r htmlextra \
      --reporter-htmlextra-export "postman/reports/report-$TIMESTAMP.html" \
      --reporter-htmlextra-title "Loto Bingo API Tests" \
      --reporter-htmlextra-darkTheme \
      --delay-request 200 > /dev/null 2>&1

    if [ $? -eq 0 ]; then
        echo "✅ Rapport HTML: postman/reports/report-$TIMESTAMP.html"
    fi
fi

echo ""
echo "======================================"

# Arrêter l'application si elle a été démarrée par le script
if [ "$STARTED_BY_SCRIPT" = "true" ]; then
    echo "🛑 Arrêt de l'application..."
    kill $APP_PID 2>/dev/null
    wait $APP_PID 2>/dev/null
fi

# Vérifier le résultat
if [ $TEST_RESULT -eq 0 ]; then
    echo "✅ Tous les tests sont passés avec succès!"
    echo "======================================"
    exit 0
else
    echo "❌ Certains tests ont échoué"
    echo "📄 Consultez les rapports dans: postman/reports/"
    echo "======================================"
    exit 1
fi
