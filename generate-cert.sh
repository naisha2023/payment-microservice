#!/bin/bash
set -e
set -a
source .env
set +a

CPP_GATEWAY_DIR="/media/D_arquivos/fintech/fintech-gateway"

mkdir -p certs/
mkdir -p auth-service/src/main/resources/certs/
mkdir -p notification-service/src/main/resources/certs/
mkdir -p payment-service/src/main/resources/certs/
mkdir -p wallet-service/src/main/resources/certs/
mkdir -p "$CPP_GATEWAY_DIR/certs"

# ── CA: solo se genera si no existe ──────────────────────────
if [ ! -f certs/ca.key ] || [ ! -f certs/ca.crt ]; then
    echo "🔐 Generando CA por primera vez..."
    openssl genrsa -out certs/ca.key 4096
    openssl req -new -x509 -days 3650 \
        -key certs/ca.key \
        -out certs/ca.crt \
        -subj "/CN=Fintech-CA"
    # Resetea el serial cuando el CA es nuevo
    rm -f certs/ca.srl
else
    echo "✅ CA ya existe, reutilizando..."
fi

# ── Genera cert firmado por el CA actual ──────────────────────
generate_cert() {
    NAME="$1"
    CN="$2"
    echo "🔐 Generando $NAME..."
    openssl genrsa -out "certs/$NAME.key" 2048
    cat > "certs/$NAME.ext" <<EOF
subjectAltName = DNS:$CN
extendedKeyUsage = serverAuth, clientAuth
EOF
    openssl req -new \
        -key "certs/$NAME.key" \
        -out "certs/$NAME.csr" \
        -subj "/CN=$CN"
    openssl x509 -req -days 3650 \
        -in "certs/$NAME.csr" \
        -CA certs/ca.crt \
        -CAkey certs/ca.key \
        -CAcreateserial \
        -out "certs/$NAME.crt" \
        -extfile "certs/$NAME.ext"
    cat "certs/$NAME.key" "certs/$NAME.crt" > "certs/$NAME.pem"
}

generate_cert "server"  "auth-service"
generate_cert "wallet"  "wallet-service"
generate_cert "payment" "payment-service"
generate_cert "client"  "notification-service"
generate_cert "gateway" "api-gateway"

echo "📦 Generando PKCS12..."
openssl pkcs12 -export \
    -in certs/server.crt -inkey certs/server.key \
    -out certs/server.p12 -name server \
    -CAfile certs/ca.crt -password pass:${SSL_PASSWORD}

openssl pkcs12 -export \
    -in certs/client.crt -inkey certs/client.key \
    -out certs/client.p12 -name client \
    -CAfile certs/ca.crt -password pass:${SSL_PASSWORD}

openssl pkcs12 -export \
    -in certs/payment.crt -inkey certs/payment.key \
    -out certs/payment.p12 -name payment \
    -CAfile certs/ca.crt -password pass:${SSL_PASSWORD}

openssl pkcs12 -export \
    -in certs/wallet.crt -inkey certs/wallet.key \
    -out certs/wallet.p12 -name wallet \
    -CAfile certs/ca.crt -password pass:${SSL_PASSWORD}

openssl pkcs12 -export \
    -in certs/gateway.crt -inkey certs/gateway.key \
    -out certs/gateway.p12 -name gateway \
    -CAfile certs/ca.crt -password pass:${SSL_PASSWORD}

echo "🔒 Generando truststore..."
keytool -import -alias ca \
    -file certs/ca.crt \
    -keystore certs/truststore.p12 \
    -storetype PKCS12 \
    -storepass ${SSL_TRUST_STORE_PASSWORD} \
    -noprompt

cp certs/ca.crt certs/truststore.pem

echo "📂 Copiando certs a servicios Spring..."
cp certs/server.p12     auth-service/src/main/resources/certs/
cp certs/truststore.p12 auth-service/src/main/resources/certs/
cp certs/client.p12     notification-service/src/main/resources/certs/
cp certs/truststore.p12 notification-service/src/main/resources/certs/
cp certs/payment.p12    payment-service/src/main/resources/certs/
cp certs/truststore.p12 payment-service/src/main/resources/certs/
cp certs/wallet.p12     wallet-service/src/main/resources/certs/
cp certs/truststore.p12 wallet-service/src/main/resources/certs/

echo "📂 Copiando certs PEM al gateway C++..."
cp certs/gateway.pem    "$CPP_GATEWAY_DIR/certs/"
cp certs/gateway.crt    "$CPP_GATEWAY_DIR/certs/"
cp certs/gateway.key    "$CPP_GATEWAY_DIR/certs/"
cp certs/truststore.pem "$CPP_GATEWAY_DIR/certs/"
cp certs/ca.crt         "$CPP_GATEWAY_DIR/certs/"

echo "✅ Certificados generados correctamente"
echo "Gateway C++ certs en: $CPP_GATEWAY_DIR/certs"