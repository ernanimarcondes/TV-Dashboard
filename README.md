# TV-Dashboard

Uma tela inicial para Android TV, com fundo preto, cartões em grafite, rosa e roxo, esportes, clima, vídeos de canais escolhidos e a música em reprodução no Spotify da própria TV.

> **Baseado no [Free My TV, de simbasjungles e seus colaboradores](https://github.com/simbasjungles/free-my-tv).** Este projeto adapta a interface Android e mantém a infraestrutura de instalação, alternância da tela inicial e recuperação do projeto original. A licença MIT original está preservada em [LICENSE](LICENSE).

## O painel

- Palmeiras e Boston Celtics: próximo jogo, escudos e um recorte da classificação.
- Fórmula 1: próximo GP, traçado do circuito, treinos, classificação, Sprint quando disponível e corrida.
- Spotify: capa, música e artista publicados pela sessão de mídia local do Android. O painel não solicita email, senha ou token da conta Spotify.
- YouTube: novidades dos canais configurados, em ordem de publicação, com um vídeo por vez, miniatura e título. Transição lateral automática a cada 12 segundos, pausada quando o cartão está selecionado.
- Clima: temperatura atual e previsão da cidade escolhida.
- Atalhos para aplicativos, TV/HDMI e ajustes.

A primeira configuração de esportes é Palmeiras, Celtics e F1. O código permite personalização. As informações não são placares ao vivo. Fontes públicas podem mudar, ficar indisponíveis ou apresentar programação desatualizada.

## Perfis de painel

Em **Ajustes → Selecionar painel**, escolha **Dash Pessoal**, **Esportes Dash** ou **Original**. O Dash Pessoal preserva o painel existente. Esportes Dash é, por enquanto, uma tela de preparação para o próximo painel. O aplicativo lembra o último perfil escolhido; os aplicativos e as contas continuam compartilhados.

A escolha também fica em **Free My TV → Home Screen**. Trocar entre os dois painéis não altera o modo da TV; Original continua usando o mecanismo de restauração do projeto de origem.

## Compatibilidade

Android TV 8 ou superior. A interface foi testada em uma TCL C06 com Android 11 e resolução de interface 1280×720. O desenho é vetorial onde possível e ajusta o painel à resolução disponível. Compatibilidade com outros firmwares precisa ser verificada; instalar o APK não é o mesmo que habilitar a substituição da tela inicial.

Este repositório contém a adaptação Android. As versões LG webOS e Samsung Tizen do projeto de origem não fazem parte deste painel.

## Compilar

Requisitos: Python 3, JDK 17 ou superior, Android SDK com `build-tools;34.0.0` e `platforms;android-34`.

Defina `JAVA_HOME` e `ANDROID_HOME` para as instalações locais e execute:

```text
python build.py
```

O resultado é `out/TV-Dashboard.apk`. O build padrão utiliza configurações públicas genéricas e uma chave de desenvolvimento local, criada em `.local/debug.keystore`. Nem essa chave nem os arquivos de saída são versionados. O build padrão ignora `config.properties` e sua seleção local de canais. Não há instalação automática na TV nem upload de arquivos durante a compilação.

Para uma compilação pessoal:

1. Copie `config.example.properties` para `config.properties` e preencha a cidade/coordenadas se desejar; a localização também pode ser escolhida nas configurações do Free My TV.
2. Copie `app/assets/racing/youtube-channels.example.json` para `app/assets/racing/youtube-channels.json` e substitua o exemplo pelos IDs públicos dos canais desejados. Mantenha `videos: []`; os uploads serão consultados pelo aplicativo.
3. Execute `python build.py --personal`.

Os dois arquivos pessoais são ignorados pelo Git. A compilação pública acompanha somente o canal oficial Formula 1 como exemplo, sem a lista pessoal de quem criou esta adaptação. Nenhum login no YouTube é necessário para consultar os feeds públicos.

## Instalar e ativar

Ative as opções de desenvolvedor e a depuração na TV. Use o [ADB oficial do Android](https://developer.android.com/tools/releases/platform-tools), conecte a TV e autorize o computador na tela:

```text
adb connect ENDERECO_DA_TV:5555
adb install -r out/TV-Dashboard.apk
```

Em alguns firmwares TCL, é necessário permitir a execução em segundo plano:

```text
adb shell "appops set io.freemytv AUTO_START allow || appops set io.freemytv APP_AUTO_START allow"
```

Na TV, abra **Free My TV → Home Screen → Free My TV** e autorize a chave de depuração do aplicativo. A troca mantém a arquitetura de recuperação do projeto original e pode desativar o launcher original e alguns aplicativos de sistema. Leia as [instruções do projeto de origem](https://github.com/simbasjungles/free-my-tv#install) antes de ativar. Não use listas genéricas de remoção de aplicativos de sistema.

Para o cartão de reprodução, habilite o widget Now Playing nas configurações; ele utiliza o acesso de sessão de mídia via componente de notificação do Android. Em TVs que exigirem a autorização via ADB:

```text
adb shell cmd notification allow_listener io.freemytv/io.freemytv.NowPlaying
```

O pacote continua sendo `io.freemytv`. Um APK assinado com uma chave diferente não atualiza o instalado. **Antes de desinstalar ou trocar de assinatura, volte ao modo Original.** Guarde sua chave local para futuras atualizações; não a publique.

Para voltar à tela da fabricante, escolha **Ajustes → Free My TV → Home Screen → Original**. Não são incluídas neste projeto as desativações locais feitas no aparelho de desenvolvimento.

## Privacidade

O repositório não contém contas, emails pessoais, senhas, cookies, tokens OAuth, sessões do YouTube/Spotify, chaves de assinatura da TV, capturas de reprodução ou configurações pessoais de localização e canais.

O Spotify é consultado pela sessão de mídia que o aplicativo publica no Android; não há acesso ao banco de dados ou às credenciais do aplicativo Spotify. O YouTube utiliza somente canais e vídeos públicos. O login nos aplicativos continua sob responsabilidade dos aplicativos oficiais.

O painel acessa serviços externos para fornecer os recursos escolhidos: ESPN, Jolpica F1, Open-Meteo, YouTube e repositórios de geometria no GitHub. A consulta do clima informa as coordenadas configuradas à Open-Meteo. Esses serviços recebem as requisições normais de rede. Integrações opcionais herdadas do Free My TV só devem ser configuradas com suas próprias chaves locais.

Veja [PRIVACY.md](PRIVACY.md). Antes de publicar alterações, execute:

```text
python tools/privacy_check.py
```

Essa verificação ajuda a impedir arquivos indevidos e padrões conhecidos de credenciais; não substitui revisão humana.

## Créditos e licenças

- **Free My TV:** [simbasjungles/free-my-tv](https://github.com/simbasjungles/free-my-tv), licença MIT. Código-base na revisão `0e33f168b82ffb05d002b3ab9093f12048bdfc92`. O [README original](docs/UPSTREAM-README.md) foi preservado como referência.
- **Traçados:** [bacinger/f1-circuits](https://github.com/bacinger/f1-circuits), MIT.
- **Fontes:** Inter e Rajdhani, SIL Open Font License. Avisos incluídos em `app/assets/fonts`.
- **Ícones:** Material Symbols; aviso de licença incluído nos assets.
- **Dados:** ESPN, Jolpica F1, Open-Meteo e feeds públicos YouTube.

Escudos, capas, miniaturas e nomes de serviços pertencem aos respectivos titulares. Veja [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Sem afiliação com TCL, Google, Spotify, YouTube ou os campeonatos e equipes citados.

<img width="1280" height="720" alt="image" src="https://github.com/user-attachments/assets/d372d26e-bb92-4f29-b168-1b4472aa1910" />
