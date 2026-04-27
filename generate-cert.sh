#!/bin/bash
set -e
set -a
source .env
set +a

echo "🧹 Limpiando certs anteriores..."
rm -rf certs/
mkdir -p certs/
mkdir -p auth-service/src/main/resources/certs/
mkdir -p notification-service/src/main/resources/certs/
mkdir -p payment-service/src/main/resources/certs/
mkdir -p wallet-service/src/main/resources/certs/
mkdir -p api-gateway/src/main/resources/certs/

echo "🔐 Generando CA..."
openssl genrsa -out certs/ca.key 4096
openssl req -new -x509 -days 365 -key certs/ca.key -out certs/ca.crt -subj "/CN=MyCA"

echo "🗄️ Generando certificado auth-service..."
openssl genrsa -out certs/server.key 2048
openssl req -new -key certs/server.key -out certs/server.csr -subj "/CN=auth-service"
openssl x509 -req -days 365 -in certs/server.csr -CA certs/ca.crt -CAkey certs/ca.key -CAcreateserial -out certs/server.crt

echo "🗄️ Generando certificado notification-service..."
openssl genrsa -out certs/client.key 2048
openssl req -new -key certs/client.key -out certs/client.csr -subj "/CN=notification-service"
openssl x509 -req -days 365 -in certs/client.csr -CA certs/ca.crt -CAkey certs/ca.key -CAcreateserial -out certs/client.crt

echo "🗄️ Generando certificado payment-service..."
openssl genrsa -out certs/payment.key 2048
openssl req -new -key certs/payment.key -out certs/payment.csr -subj "/CN=payment-service"
openssl x509 -req -days 365 -in certs/payment.csr -CA certs/ca.crt -CAkey certs/ca.key -CAcreateserial -out certs/payment.crt

echo "🗄️ Generando certificado wallet-service..."
openssl genrsa -out certs/wallet.key 2048
openssl req -new -key certs/wallet.key -out certs/wallet.csr -subj "/CN=wallet-service"
openssl x509 -req -days 365 -in certs/wallet.csr -CA certs/ca.crt -CAkey certs/ca.key -CAcreateserial -out certs/wallet.crt

echo "🗄️ Generando certificado api-gateway..."
openssl genrsa -out certs/gateway.key 2048
openssl req -new -key certs/gateway.key -out certs/gateway.csr -subj "/CN=api-gateway"
openssl x509 -req -days 365 -in certs/gateway.csr -CA certs/ca.crt -CAkey certs/ca.key -CAcreateserial -out certs/gateway.crt

echo "📦 Convirtiendo a PKCS12..."
openssl pkcs12 -export \
  -in certs/server.crt \
  -inkey certs/server.key \
  -out certs/server.p12 \
  -name server \
  -CAfile certs/ca.crt \
  -password pass:${SSL_PASSWORD}

openssl pkcs12 -export \
  -in certs/client.crt \
  -inkey certs/client.key \
  -out certs/client.p12 \
  -name client \
  -CAfile certs/ca.crt \
  -password pass:${SSL_PASSWORD}

openssl pkcs12 -export \
  -in certs/payment.crt \
  -inkey certs/payment.key \
  -out certs/payment.p12 \
  -name payment \
  -CAfile certs/ca.crt \
  -password pass:${SSL_PASSWORD}

openssl pkcs12 -export \
  -in certs/wallet.crt \
  -inkey certs/wallet.key \
  -out certs/wallet.p12 \
  -name wallet \
  -CAfile certs/ca.crt \
  -password pass:${SSL_PASSWORD}

openssl pkcs12 -export \
  -in certs/gateway.crt \
  -inkey certs/gateway.key \
  -out certs/gateway.p12 \
  -name gateway \
  -CAfile certs/ca.crt \
  -password pass:${SSL_PASSWORD}

echo "🔒 Generando truststore..."
keytool -import -alias ca \
  -file certs/ca.crt \
  -keystore certs/truststore.p12 \
  -storetype PKCS12 \
  -storepass ${SSL_TRUST_STORE_PASSWORD} \
  -noprompt

echo "✅ Verificando certs..."
openssl pkcs12 -info -in certs/server.p12 -passin pass:${SSL_PASSWORD} -noout
openssl pkcs12 -info -in certs/client.p12 -passin pass:${SSL_PASSWORD} -noout
openssl pkcs12 -info -in certs/payment.p12 -passin pass:${SSL_PASSWORD} -noout
openssl pkcs12 -info -in certs/wallet.p12 -passin pass:${SSL_PASSWORD} -noout
openssl pkcs12 -info -in certs/gateway.p12 -passin pass:${SSL_PASSWORD} -noout
openssl pkcs12 -info -in certs/truststore.p12 -passin pass:${SSL_TRUST_STORE_PASSWORD} -noout

echo "📂 Copiando a los servicios..."
cp certs/server.p12     auth-service/src/main/resources/certs/
cp certs/truststore.p12 auth-service/src/main/resources/certs/

cp certs/client.p12     notification-service/src/main/resources/certs/
cp certs/truststore.p12 notification-service/src/main/resources/certs/

cp certs/payment.p12    payment-service/src/main/resources/certs/
cp certs/truststore.p12 payment-service/src/main/resources/certs/

cp certs/wallet.p12     wallet-service/src/main/resources/certs/
cp certs/truststore.p12 wallet-service/src/main/resources/certs/

cp certs/gateway.p12    api-gateway/src/main/resources/certs/
cp certs/truststore.p12 api-gateway/src/main/resources/certs/

echo "🎉 Certificados generados y copiados correctamente"