# PLANO-DE-IMPLEMENTACAO.md — FTC 31211

**Projeto:** `MannaTeam-FTC-31211-2026`  
**Temporada:** FTC 2026 — BIOBUZZ  
**Status do plano:** Revisão consolidada proposta; aguarda validação do responsável  
**Escopo:** Base universal da equipe e camada específica BIOBUZZ  
**Código Java alterado durante esta revisão:** Não  
**Última revisão:** 2026-09-29

---

## 1. Objetivo e regras de trabalho

Este plano converte os achados das auditorias AUD-001 a AUD-010 em uma sequência de trabalho incremental. A arquitetura deve ser modular, compreensível para estudantes, reutilizável entre temporadas e proporcional às necessidades reais do robô.

A execução deve seguir este ciclo por etapa:

1. Inspecionar o código e os consumidores afetados.
2. Registrar o comportamento atual e o resultado esperado.
3. Separar fatos confirmados, hipóteses e decisões em aberto.
4. Aprovar a mudança proposta quando ela alterar contratos ou comportamento operacional.
5. Implementar a menor alteração suficiente.
6. Revisar o diff e compilar.
7. Executar testes de código e, quando aplicável, testes controlados no robô.
8. Registrar evidências e pendências.
9. Avançar somente após validação da etapa.

Não substituir classes em lote, não remover métodos utilizados por consumidores existentes sem plano de migração e não afirmar validação física sem evidência de teste no robô.

## 2. Separação arquitetural pretendida

### 2.1 Base universal da equipe

- Hardware e configuração;
- Drivetrain e subsistemas reutilizáveis;
- Controle PID e feedforward quando houver necessidade demonstrada;
- Localization;
- Vision;
- Commands e execução de ações;
- Composição e ciclo de vida de Robot;
- Telemetria;
- Integrações externas;
- Testes, ferramentas e documentação.

### 2.2 Camada específica da temporada BIOBUZZ

- Regras, estratégia e pontuação;
- Posições de campo e rotas;
- OpModes competitivos;
- Commands exclusivos do jogo;
- Mecanismos exclusivos da temporada;
- Constantes específicas do jogo.

Exemplo: `Shooter` deve modelar o mecanismo reutilizável; uma ação como `ScoreHiveCommand`, se necessária e alinhada às regras do jogo, pertence à camada de temporada.

A separação é uma direção arquitetural proposta, não uma autorização para mover classes antes de inspecionar os consumidores e dependências atuais.

## 3. Estados e prioridades

### 3.1 Estado de cada item

- **Proposto:** solução sugerida, ainda não aprovada.
- **Aprovado:** decisão aceita pelo responsável do projeto.
- **Implementado:** alteração de código concluída.
- **Validado:** critérios técnicos e, quando aplicáveis, físicos foram comprovados.
- **Bloqueado:** falta arquivo, especificação, decisão ou acesso necessário.

Compilar não equivale a validar comportamento. Teste simulado não equivale a validação física.

### 3.2 Prioridades

- **P0 — Estabilização operacional:** defeito confirmado que pode impedir a execução ou provocar atuação física inesperada.
- **P1 — Contratos e correções importantes:** decisões ou mudanças necessárias para integração coerente.
- **P2 — Melhoria posterior:** refatoração ou funcionalidade que não bloqueia a estabilização.

A prioridade deve refletir severidade e dependências. Uma pendência arquitetural não é automaticamente P0.

## 4. Estado documental das auditorias

O estado a seguir é o registro de trabalho fornecido para esta revisão; deve ser confrontado com os documentos originais antes de ser considerado uma transcrição integral deles.

| Auditoria | Estado de trabalho | Pendências principais |
|---|---|---|
| AUD-001 — Robot Core | Auditoria concluída | Definir composição, ciclo de vida e estratégia de configuração |
| AUD-002 — Drivetrain | Auditoria concluída | Movimento por encoder, IMU, PID, RunMode e fronteira com Localization |
| AUD-003 — Intake | Aprovado com ressalvas | Atualização periódica, estados, `isBusy()` e parada |
| AUD-004 — Shooter | Aprovado estruturalmente | Confirmar parâmetros físicos, limites de RPM e intenção do valor manual |
| AUD-005 — Viper | Baseline documentado | Especificação mecânica e hardware ainda necessários |
| AUD-006 — Localization | Integração de hardware existente | API de pose, unidades, convenções, inicialização e atualização |
| AUD-007 — Vision | Estágio inicial funcional | Contrato de ausência de alvo e integração futura com Localization |
| AUD-008 — Control & Commands | PID básico existente | Contrato de PID, ciclo de vida de Commands e executor |
| AUD-009 — Utils | `MathU` aprovado; `CalcDistAlvo` com ressalva | Confirmar consumidores e fronteiras de responsabilidade |
| AUD-010 — OpModes | Problemas operacionais identificados | Investigar NovoTeleop, CalibrarDistanciaRPM e entradas compartilhadas |

**Importante:** esses estados descrevem auditorias e pendências, não o estado de implementação atual. A versão dos arquivos Java no repositório deve ser verificada antes de cada alteração.

## 5. Normalização documental — pendente de aplicação nos arquivos originais

Foram relatadas as seguintes inconsistências de identificação:

- `AUD-006-Localization.md` termina com uma referência a AUD-005;
- A conclusão de `AUD-007-Vision.md` contém um identificador incorreto;
- `AUD-009-Utils.md` usa AUD-008 para identificar a própria auditoria.

Correção documental proposta, sem alterar conclusões técnicas:

1. Manter Viper como AUD-005.
2. Identificar Localization como AUD-006 em título, conclusão e referências internas que identifiquem a própria auditoria.
3. Identificar Vision como AUD-007.
4. Identificar Control & Commands como AUD-008.
5. Identificar Utils como AUD-009.
6. Identificar OpModes como AUD-010.
7. Pesquisar referências cruzadas nos dez documentos e no plano para evitar substituir números que façam parte de referências legítimas a outras auditorias.

**Estado:** proposta documental. Os arquivos originais das auditorias não foram modificados nesta revisão. A correção só pode ser marcada como implementada depois de editar e revisar os arquivos reais.

## 6. Fase 0 — Estabilização operacional dos OpModes

**Prioridade:** P0 para os riscos confirmados; demais itens permanecem sujeitos à inspeção.  
**Estado:** Proposto — inspeção do código atual necessária.  
**Objetivo:** corrigir riscos localizados sem antecipar a arquitetura completa.

### 6.1 `NovoTeleop`

**Relatos da auditoria a confirmar no código atual:**
- chamada `odo.init(hardwareMap)` sem instanciação anterior de `odo`;
- lógica experimental capaz de interferir no controle normal;
- organização do controle com possíveis conflitos.

**Procedimento:**
1. Abrir o arquivo atual e rastrear declaração, instanciação e uso de `odo`.
2. Pesquisar todos os consumidores e chamadas aos métodos envolvidos.
3. Determinar, a partir do código e da intenção documentada, se a odometria é necessária nesse OpMode.
4. Se necessária, corrigir a instanciação com escopo mínimo; caso contrário, retirar apenas a dependência comprovadamente desnecessária.
5. Isolar ou remover lógica experimental somente após identificar seu comportamento e confirmar que não é uma funcionalidade operacional necessária.
6. Revisar os caminhos de controle do drivetrain para garantir que uma ação de teste não sobrescreva o comando normal.

**Arquivos candidatos:** `opmodes/NovoTeleop.java` e somente as classes diretamente envolvidas, se necessário.  
**Riscos:** remover comportamento de campo ainda necessário; introduzir inicialização duplicada.  
**Testes:** inspeção estática, compilação, teste inicial com rodas suspensas ou drivetrain desabilitado, depois teste controlado.  
**Critérios de conclusão:**
- não há acesso a `odo` antes da inicialização;
- não há comando experimental involuntário no fluxo normal;
- a funcionalidade preservada está documentada;
- compilação e teste correspondente registrados.

### 6.2 `CalibrarDistanciaRPM`

**Relato a confirmar:** a ferramenta informa que não deveria acionar o drivetrain, mas chama `updateDrive()`.

**Procedimento:**
1. Inspecionar a implementação completa de `updateDrive()` e sua cadeia de chamadas.
2. Confirmar se ela pode escrever potência nos motores ou alterar modos/targets.
3. Identificar o comportamento pretendido da ferramenta a partir do código, telemetria e documentação.
4. Se a ferramenta deve ser estacionária, impedir a chamada que pode comandar o drivetrain e garantir potência zero na inicialização e encerramento.
5. Se alinhamento motorizado for uma função intencional, tratá-lo como requisito explícito, com controles e telemetria coerentes — não presumir essa intenção.

**Arquivos candidatos:** `opmodes/CalibrarDistanciaRPM.java` e consumidores diretos de `updateDrive()`.  
**Riscos:** deixar motores em potência anterior ou remover alinhamento necessário sem confirmar a intenção.  
**Testes:** verificar todos os caminhos de entrada e saída; teste com rodas suspensas e observação da potência comandada.  
**Critério de conclusão:** o comportamento real corresponde à finalidade declarada e não existe acionamento não intencional do drivetrain.

### 6.3 Conflito de `gamepad2.x`

**Relato a confirmar:** o botão é usado tanto para alinhamento quanto para reset dos encoders do Shooter.

**Procedimento:**
1. Localizar todas as leituras de `gamepad2.x` no OpMode e nas funções chamadas.
2. Registrar as ações que cada leitura dispara e se ocorrem no mesmo ciclo.
3. Definir interação inequívoca com base nas funcionalidades necessárias.
4. Alterar somente o mapeamento conflitante e atualizar a telemetria/instruções ao operador.
5. Testar cada ação individualmente e verificar que não dispara a outra.

**Critério de conclusão:** cada ação possui gatilho claro, sem colisão involuntária; os comportamentos necessários continuam acessíveis.

### Saída obrigatória da Fase 0

Registro por problema: código antes/depois, arquivos alterados, consumidores examinados, compilação, testes executados, resultado e pendências. Não marcar como validado sem evidência.

## 7. Fase 1 — Decisões arquiteturais e contratos

**Prioridade:** P1.  
**Estado:** Proposto — decisões não aprovadas até validação do responsável.  
**Objetivo:** decidir contratos antes de preencher classes ou migrar parâmetros.

### 7.1 Configuração

Direção proposta:

- `DrivetrainConfig`: nomes dos motores e parâmetros físicos/calibrações/controle específicos do drivetrain.
- `ShooterConfig`: parâmetros do Shooter, após confirmar unidades e finalidade dos valores.
- `IntakeConfig`: manter apenas se existir configuração própria suficiente para justificar a classe.
- `ViperConfig`: somente quando mecanismo e hardware forem especificados.
- `HardwareConfig`: manter apenas se possuir responsabilidade compartilhada distinta, não apenas um segundo local para constantes.
- `RobotConfig`: manter apenas se representar composição/configuração global útil e distinta.
- `RConstants`: deixar de crescer como depósito geral, migrando por grupos coerentes após pesquisa de consumidores.

**Não migrar constantes isoladamente nesta fase.** Primeiro elaborar inventário: constante, unidade, valor atual, consumidores, categoria, evidência física e destino proposto.

**Decisão em aberto:** necessidade de `HardwareConfig` e `RobotConfig`.  
**Critério:** cada classe mantida possui responsabilidade própria demonstrável; não existem duplicações de fonte de verdade.

### 7.2 `Robot` e ciclo de vida

Definir construção, inicialização, atualização e encerramento antes de implementar `Robot`.

Contrato proposto:
- inicializar apenas os componentes habilitados e necessários;
- possuir um ponto claro para atualizar subsistemas que necessitem de `update()`;
- encaminhar parada/interrupção para os componentes pertinentes;
- não duplicar configuração de hardware entre `Robot` e subsistemas.

**Decisão em aberto:** quais componentes serão compostos por `Robot` e quais dependem de um OpMode ou ferramenta especializada.

### 7.3 Commands e executor

Comparar FTC SDK puro, executor próprio mínimo e NextFTC usando a versão/dependências reais do projeto. Não adotar framework por popularidade.

Se for implementado um contrato próprio, avaliar o ciclo:
`initialize()` → `execute()` repetido → `isFinished()` → `end()`.

Definir cancelamento, timeout, falha, concorrência por subsistema, sequenciamento e parada segura. Não forçar Commands para operações simples que devam continuar como métodos de subsistemas.

### 7.4 Atualização periódica

O Intake depende de chamadas periódicas a `update()` para concluir a alimentação temporizada. Definir quem atualiza cada subsistema, em qual ciclo e como a atualização é testada. Evitar que cada OpMode implemente o mesmo ciclo separadamente.

### 7.5 PID

O controlador deve continuar genérico e independente de hardware. Antes de alterar sua fórmula, definir unidades, `dt`, saturação, tratamento da integral, reset, limites de saída e critérios de término. Uma mudança de fórmula pode alterar o comportamento atual; ganhos antigos não devem ser tratados como equivalentes sem testes e calibração.

**Critério de conclusão da Fase 1:** decisões documentadas e aprovadas; nenhuma alteração de código necessária para declarar a fase aprovada.

## 8. Fase 2 — Localization, Pinpoint, IMU e Drivetrain

**Prioridade:** P1.  
**Estado:** Proposto; validação física pendente.  
**Objetivo:** definir e implementar uma única fronteira para pose e movimento sem remover primitivas existentes prematuramente.

### 8.1 Contrato de Localization

Definir antes da implementação:
- representação de pose;
- unidades;
- convenções de X, Y e heading;
- origem e orientação do campo;
- direção e resolução dos encoders dos pods;
- offsets;
- inicialização, atualização e reset;
- efeito do reset sobre pose e heading;
- responsabilidade pela orientação;
- relação entre IMU do Hub e IMU interna do Pinpoint.

A inicialização e atualização do Pinpoint devem ter um único responsável. Não duplicar a inicialização em Drivetrain e Localization.

### 8.2 Drivetrain

Preservar inicialmente o controle mecanum e os métodos de movimento por encoder utilizados pelos consumidores atuais.

Investigar:
- acesso residual ao Pinpoint dentro de Drivetrain;
- contrato de `RunMode` antes, durante e após `RUN_TO_POSITION`;
- configuração física da IMU;
- direção dos motores e sinais dos encoders;
- `encoderDriveCm()`, `encoderStrafeCm()` e `encoderTurnCm()`;
- `STRAFE_CORRECTION` e calibração física;
- semântica e consumidores de `isBusy()`.

**Regra de preservação:** manter a semântica atual de `isBusy()` baseada em `OR` até haver evidência e testes que justifiquem mudança. Não converter para `AND` por preferência de implementação.

Encoders de motor, pods de odometria, estimativa de pose e acompanhamento de trajetória são conceitos distintos e não devem ser confundidos.

### 8.3 Pedro Pathing

A integração é pretendida, mas a versão e as APIs precisam ser confirmadas no projeto antes de implementar um adaptador.

Verificar:
- dependência e versão instaladas;
- compatibilidade e API de localizer;
- forma de fornecer pose;
- forma de enviar saída de controle ao drivetrain;
- convenções de unidades e coordenadas;
- integração com comandos e ciclo de vida.

Fronteira pretendida:
`Localization → pose`  
`Pedro Pathing → acompanhamento de trajetória`  
`Drivetrain → acionamento dos motores`

Não substituir imediatamente os métodos por encoder. Primeiro integrar e testar uma trajetória pequena; depois decidir o que permanece como primitiva de baixo nível.

### Testes e critérios da Fase 2

- API de pose documentada e usada pelos consumidores;
- Pinpoint inicializado/atualizado por um único responsável;
- unidades, offsets e direções conferidos;
- reset e heading verificados;
- movimentos mecanum e por encoder preservados;
- comportamento de `RunMode` verificado;
- `isBusy()` preservado ou alteração justificada por evidência;
- validação física da pose e dos pods registrada;
- integração Pedro só considerada validada após teste real e compatível com a versão instalada.

## 9. Fase 3 — Composição e execução

**Prioridade:** P1.  
**Dependências:** contratos da Fase 1 e fronteiras necessárias da Fase 2 aprovados.  
**Estado:** Proposto.

### Arquivos candidatos
`robot/Robot.java`, `config/RobotConfig.java`, `config/HardwareConfig.java`, `commands/Command.java` e somente os componentes necessários para atualizar/parar.

### Trabalho
- implementar composição de Robot conforme responsabilidades aprovadas;
- implementar executor mínimo apenas se a análise confirmar sua necessidade;
- centralizar a atualização periódica;
- integrar cancelamento, timeout e parada;
- evitar duplicação de inicialização em OpModes.

### Riscos
- abstração maior que o necessário;
- alterar a ordem de inicialização que os mecanismos atuais exigem;
- comandos concorrentes controlarem o mesmo mecanismo.

### Testes e critérios
- inicialização e encerramento consistentes;
- subsistemas atualizados uma única vez por ciclo previsto;
- operações temporizadas terminam ou são canceladas corretamente;
- timeout e interrupção produzem estado seguro;
- OpModes migrados gradualmente, sem exigir reescrita em lote.

## 10. Fase 4 — Subsistemas e OpModes

**Prioridade:** P1/P2 por item.  
**Estado:** Proposto, com dependências específicas.

### 10.1 Intake
Manter o comportamento atual enquanto a composição é implementada. Depois revisar estados, `isBusy()`, contrato de atualização, parada e necessidade de `IntakeConfig`. Validar fisicamente o tempo de alimentação; 260 ms é parâmetro configurado, não prova de desempenho.

### 10.2 Shooter
Não reescrever o subsistema sem evidência de defeito. Antes da calibração formal confirmar modelo exato do motor, ticks por revolução, RPM nominal, relação mecânica motor/rolo, unidades, limites seguros, critério de prontidão e coordenação com Intake/Feeder.

A discrepância `MANUAL_SHOOTER_RPM = 20.0` e `MIN_SHOOTER_RPM = 300.0` deve ser rastreada até os consumidores de `setRPM()`. Manter o valor emergencial existente até confirmar sua intenção; resolver a semântica antes da calibração formal, sem classificá-la antecipadamente como bug.

### 10.3 Viper
**Bloqueado para implementação funcional** até documentar função mecânica, hardware, sensores, curso, posições, limites e condições de segurança. Essa pendência não deve bloquear trabalho independente nos demais subsistemas.

### 10.4 OpModes
Migrar gradualmente para os contratos aprovados. Os OpModes existentes são referência de uso atual, não necessariamente a arquitetura final. Preservar rotas e comportamentos relevantes; separar estratégia BIOBUZZ da lógica universal dos mecanismos.

### Critérios
- API e responsabilidades documentadas;
- consumidores atualizados sem remoções abruptas;
- testes de regressão executados;
- calibrações físicas registradas;
- Viper não marcado como implementado sem requisitos mecânicos e testes.

## 11. Fase 5 — Vision, Utils e integrações

**Prioridade:** P2, salvo risco operacional novo confirmado.  
**Estado:** Proposto.

### Vision
Preservar a implementação existente enquanto funcional. Documentar a semântica de ausência de alvo e confirmar que consumidores verificam presença/validade antes de usar medidas. Não implementar fusão visual com Localization sem contrato de coordenadas, validade e critérios de aceitação.

### Utils
Manter `MathU` como utilitário matemático, sujeito a testes de seus consumidores. Manter `CalcDistAlvo` sem refatoração ampla até definir fronteiras entre visão, localização e cálculo de tiro. Preservar parâmetros de calibração física até haver dados que justifiquem mudança.

### Bibliotecas externas
Registrar versão, função, justificativa, limites e alternativas para Pedro Pathing, NextFTC, FTCLib/SolversLib e Limelight quando forem efetivamente considerados. Evitar dependências redundantes e adaptadores sem valor demonstrado. Consultar a documentação da versão realmente instalada.

### Critérios
- ausência de alvo tratada explicitamente;
- unidades e coordenadas documentadas;
- parâmetros físicos não alterados sem evidência;
- dependências externas e versões registradas;
- integração testada em vez de presumida.

## 12. Fase 6 — Regressão, documentação e separação por temporada

**Prioridade:** obrigatória antes da homologação.  
**Estado:** Proposto.

Executar:
1. revisão de código e diff;
2. compilação;
3. testes disponíveis;
4. testes de componentes;
5. testes de integração;
6. testes funcionais controlados;
7. validação física quando aplicável;
8. registro de resultados e pendências;
9. revisão de separação entre base universal e BIOBUZZ;
10. documentação de arquitetura e sucessão.

A conclusão da fase exige evidência dos testes executados. Compilação isolada não comprova comportamento físico.

## 13. Registro das principais decisões em aberto

| Decisão | Evidência necessária | Bloqueia |
|---|---|---|
| Necessidade de `RobotConfig`/`HardwareConfig` | Inventário de consumidores e responsabilidades | Migração de configuração |
| Dono do ciclo de atualização | Inspeção dos OpModes e métodos `update()` | Integração final de Intake/Commands |
| Executor próprio ou biblioteca | Dependências reais e casos de uso | Implementação abrangente de Commands |
| Contrato de pose/heading | Código de Localization, configuração Pinpoint e convenções de campo | Integração Localization/Pedro |
| Integração Pedro | Versão instalada e APIs correspondentes | Autônomos baseados em trajetória |
| Parâmetros físicos do Shooter | Modelo de motor, encoder e relação mecânica | Calibração formal |
| Função do Viper | Especificação mecânica e hardware | Implementação do Viper |
| Intenção do RPM manual de 20 | Consumidores e contexto operacional confirmado | Calibração formal e controle manual |
| Intenção de `updateDrive()` na ferramenta | Código completo e finalidade da ferramenta | Correção definitiva de CalibrarDistanciaRPM |

## 14. Primeira execução concreta

A primeira tarefa de código deve ser uma inspeção dirigida da Fase 0, sem modificar arquivos até confirmar os consumidores e o comportamento real.

### Checklist de execução
- [ ] Confirmar branch e estado atual do repositório.
- [ ] Obter a versão atual de `NovoTeleop.java`.
- [ ] Rastrear declaração, instanciação e uso de `odo`.
- [ ] Rastrear a lógica experimental e suas escritas de potência.
- [ ] Obter a versão atual de `CalibrarDistanciaRPM.java`.
- [ ] Inspecionar `updateDrive()` e todas as suas chamadas.
- [ ] Enumerar todas as ações ligadas a `gamepad2.x`.
- [ ] Definir correções mínimas por arquivo e seus testes.
- [ ] Implementar uma correção por vez.
- [ ] Compilar e revisar o diff após cada alteração.
- [ ] Testar primeiro com risco físico reduzido; depois, teste controlado no robô.
- [ ] Atualizar o estado do plano com evidências.

**Arquivos-fonte necessários para iniciar:** `NovoTeleop.java`, `CalibrarDistanciaRPM.java` e quaisquer classes chamadas por `updateDrive()` que possam comandar os motores. Para preservar o trabalho existente, também é necessário conhecer a branch/estado atual ou receber uma cópia atual desses arquivos.

## 15. Critério global de conclusão

O plano só será considerado executado quando cada fase tiver:
- objetivo e escopo definidos;
- decisões registradas;
- arquivos alterados identificados;
- consumidores afetados revisados;
- compilação/testes documentados;
- critérios objetivos atendidos;
- pendências explícitas;
- validação do responsável pelo projeto.

**Estado geral:** Proposto — aguardando validação.  
**Auditorias:** registradas como concluídas no estado de trabalho, sujeitas à conferência documental dos arquivos originais.  
**Implementação:** não iniciada nesta revisão.  
**Próxima etapa:** inspeção dirigida da Fase 0, começando por `NovoTeleop` e `CalibrarDistanciaRPM`; nenhuma alteração Java deve ser feita sem acesso à versão atual e análise dos consumidores.
