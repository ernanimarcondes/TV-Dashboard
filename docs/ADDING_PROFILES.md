# Como adicionar um perfil

TV-Dashboard mantém os layouts separados do modo de tela inicial da TV. `HomeMode.FREE` entrega o botão Home ao aplicativo; a preferência `dashboardProfile` escolhe qual layout ele desenha. Original continua usando a recuperação do Free My TV.

## Pontos de extensão

1. Crie uma classe de layout em `app/src/io/freemytv/`, seguindo `SportsHomeView.java` (o perfil de exemplo) e os métodos de ciclo de vida de `RacingHomeView`.
2. Em `Prefs.java`, acrescente um identificador estável, valide-o em `dashboardProfile()` e `setDashboardProfile()` e forneça o nome em `dashboardName()`. Preserve `personal` como padrão e fallback.
3. Em `HomeActivity.java`, acrescente a classe à escolha de layout. Cada perfil deve suspender callbacks e serviços ao sair e liberar recursos em `destroy()`.
4. Em `SettingsActivity.java`, atualize os nomes, descrições, quantidade de linhas, marca de perfil ativo e foco de `HOME_CHOICE`. Mantenha Original como a última opção e ajuste o cálculo de `wantFree` para o novo número de perfis.
5. Confira a troca, o botão Home, a persistência da seleção e a restauração do perfil anterior. Compile com `python build.py` e execute `python tools/privacy_check.py` antes de publicar.

O seletor não duplica contas dos aplicativos, não cria usuários do Android e não depende de desativar novamente o launcher para trocar entre layouts. A criação de novos perfis é feita no código; não há um editor visual de painéis na TV.

## Exemplo esportivo

O perfil de exemplo deste repositório demonstra o isolamento do layout. A implementação esportiva completa está em [Esportes-Dash](https://github.com/ernanimarcondes/Esportes-Dash), com outro projeto e compilação própria.

## Proteções a preservar

Não altere `HomeMode`, `Watchdog`, a ordem de restauração dos aplicativos de sistema ou as verificações dos proprietários de redes Wi-Fi para acrescentar um layout. Não compartilhe dados privados, chaves de assinatura ou arquivos de contas em configurações de exemplo.
