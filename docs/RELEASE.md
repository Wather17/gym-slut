# Guia de Release e Assinatura — Gym Slut

Este guia descreve como configurar a assinatura Android (uma única vez) e publicar novas versões do **Gym Slut** via **Git Tags** e **GitHub Releases**.

---

## 1. Gerar a Keystore de Release (configuração única)

> **Importante:** guarde o arquivo `.jks` e suas senhas em local seguro (cofre de senhas / backup offline). Perder a chave impede atualizar o aplicativo por cima de instalações anteriores sem desinstalar (o que apagaria dados locais não exportados). **Nunca faça commit da keystore no Git.**

No terminal, fora do repositório (ex.: `~/keystores/`):

```bash
mkdir -p ~/keystores
keytool -genkeypair -v \
  -keystore ~/keystores/gym-slut-release.jks \
  -alias gym-slut \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

---

## 2. Configurar os GitHub Secrets

O workflow `.github/workflows/android-release.yml` exige 4 secrets no repositório:

| Secret | Conteúdo |
| :--- | :--- |
| `KEYSTORE_BASE64` | Arquivo `.jks` codificado em Base64 |
| `KEYSTORE_PASSWORD` | Senha do arquivo keystore |
| `KEY_ALIAS` | Alias da chave (ex.: `gym-slut`) |
| `KEY_PASSWORD` | Senha da chave (geralmente igual à da keystore) |

Para cadastrar usando a GitHub CLI (`gh`):

```bash
base64 -w 0 ~/keystores/gym-slut-release.jks | gh secret set KEYSTORE_BASE64
gh secret set KEYSTORE_PASSWORD
gh secret set KEY_ALIAS --body "gym-slut"
gh secret set KEY_PASSWORD
```

Você pode confirmar os nomes cadastrados com:

```bash
gh secret list
```

---

## 3. Publicar uma Nova Versão via Git Tag

O CD é disparado automaticamente ao enviar uma tag no padrão SemVer:

- **Versão estável**: `vMAJOR.MINOR.PATCH` (ex.: `v1.0.0`, `v1.0.1`, `v1.1.0`)
- **Pre-release**: `vMAJOR.MINOR.PATCH-alpha.N`, `-beta.N` ou `-rc.N` (ex.: `v1.0.0-rc.1`) — marcada automaticamente como *Pre-release* no GitHub.

### Como cortar a release a partir da `main` atualizada

```bash
git switch main
git pull --ff-only
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

O workflow **Android Release** executará:
1. Resolução de `VERSION_NAME` e `VERSION_CODE` via [scripts/resolve-release-version.sh](../scripts/resolve-release-version.sh).
2. `testDebugUnitTest` + `lintDebug`.
3. Compilação Release com **R8** (`isMinifyEnabled = true`, `isShrinkResources = true`) assinada com a keystore dos secrets.
4. Verificação criptográfica da assinatura com `apksigner verify`.
5. Publicação automática da GitHub Release anexando `gym-slut-<tag>.apk`, `gym-slut-<tag>.apk.sha256` e notas geradas automaticamente.

---

## 4. Fórmula de `versionCode` (Monotônica)

Calculada por `scripts/resolve-release-version.sh`:

`MAJOR * 1_000_000 + MINOR * 10_000 + PATCH * 100 + OFFSET`

- Estável (sem sufixo): `OFFSET = 99` (`v1.0.0` → `1000099`)
- `-rc.N`: `OFFSET = 70 + N` (`v1.0.0-rc.1` → `1000071`)
- `-beta.N`: `OFFSET = 40 + N` (`v1.0.0-beta.1` → `1000041`)
- `-alpha.N`: `OFFSET = 10 + N` (`v1.0.0-alpha.1` → `1000011`)

---

## 5. Verificar Integridade e Instalar o APK

Após baixar os dois arquivos da página de Releases do GitHub:

```bash
sha256sum -c gym-slut-v1.0.0.apk.sha256
adb install -r gym-slut-v1.0.0.apk
```
