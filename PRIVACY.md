# Privacidade e publicação

## O que este repositório inclui

Código-fonte, fontes tipográficas licenciadas, ícones, escudos públicos, traçado de exemplo, dados esportivos públicos de exemplo e configurações genéricas. Não há histórico Git importado do ambiente pessoal nem arquivos da TV.

## O que fica fora

`config.properties`, seleção pessoal de canais, `.local/`, keystores, chaves privadas, arquivos de ambiente, APKs pessoais, caches, logs, bancos de dados, capturas, vídeos e dados de sessão. O `.gitignore` cobre essas classes, e `tools/privacy_check.py` examina os arquivos rastreados antes da publicação.

A versão pública foi preparada em um diretório separado, a partir de uma lista de arquivos de código e assets. Não foram copiados os dados privados do Spotify ou do YouTube. Capturas reais da TV não são usadas na documentação, porque podem mostrar músicas, contas ou localização.

## Spotify

`SpotifyPanel.java` utiliza `MediaSessionManager` e os metadados que o aplicativo Spotify publica localmente no Android. O componente não implementa login Spotify, não lê cookies ou arquivos privados do aplicativo e não envia os metadados de reprodução a um servidor do projeto. O acesso pode ser desativado pelo widget Now Playing/configurações de acesso do Android.

## YouTube

`FavoriteYouTube.java` consulta feeds públicos pelos IDs dos canais e mantém um cache local no diretório privado do próprio aplicativo. Não lê inscrições privadas, histórico da conta, emails, senhas ou tokens de login. O build público usa uma seleção de exemplo; personalizações locais devem permanecer ignoradas pelo Git.

## Outras conexões

As coordenadas escolhidas são enviadas à Open-Meteo para o clima. Dados esportivos e imagens vêm de serviços públicos. O projeto herda integrações opcionais do Free My TV, como TMDB, Plex e Home Assistant; não há chaves pessoais fornecidas neste repositório. Uma configuração local pode alterar quais servidores são consultados.

## Limites da revisão

A verificação automatizada procura classes conhecidas de segredos e arquivos indevidos, e os arquivos publicados passaram por revisão direcionada. Isso não constitui auditoria formal de segurança nem garante que futuras alterações não introduzam problemas. Nunca publique uma chave privada de assinatura, um APK compilado com credenciais privadas ou uma captura contendo dados pessoais.
