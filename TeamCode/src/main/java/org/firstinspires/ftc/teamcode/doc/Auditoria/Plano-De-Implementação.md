# Plano de Implementação — FTC 31211

**Projeto:** MannaTeam FTC 31211 — 2026
**Documento:** `PLANO-DE-IMPLEMENTACAO.md`
**Status:** PROPOSTA — AGUARDANDO VALIDAÇÃO DO RESPONSÁVEL PELO PROJETO
**Base documental:** AUD-001 até AUD-010
**Alterações de código nesta etapa:** Nenhuma

---

## 1. Objetivo

Consolidar os resultados das dez auditorias do projeto FTC 31211 e estabelecer uma sequência de implementação baseada nos achados, nas dependências entre componentes e nos critérios necessários para validar as alterações.

O plano busca:

* preservar comportamentos existentes que as auditorias consideraram coerentes;
* corrigir problemas concretos antes de ampliar a arquitetura;
* definir responsabilidades antes de preencher classes incompletas;
* separar problemas funcionais de pendências arquiteturais;
* evitar refatorações prematuras;
* estabelecer critérios de validação técnica e física;
* manter o código operacional durante a evolução do projeto.

Este documento não autoriza automaticamente todas as recomendações das auditorias. Cada alteração deve ser justificada, implementada em uma etapa delimitada e validada antes da etapa seguinte.

## 2. Escopo e situação das auditorias

### 2.1 Auditorias incluídas

| Documento | Área                          | Situação consolidada                                                                            |
| --------- | ----------------------------- | ----------------------------------------------------------------------------------------------- |
| AUD-001   | Núcleo do robô e configuração | Composição e estratégia de configuração pendentes de definição                                  |
| AUD-002   | Drivetrain                    | Estrutura existente; decisões sobre movimento, IMU, PID e odometria pendentes                   |
| AUD-003   | Intake                        | Aprovado com ressalvas arquiteturais                                                            |
| AUD-004   | Shooter                       | Aprovado estruturalmente; parâmetros físicos precisam de confirmação antes da calibração formal |
| AUD-005   | Viper                         | Baseline; implementação depende da especificação mecânica e do hardware                         |
| AUD-006   | Localization                  | Integração de hardware existente; API de localização ainda precisa ser definida                 |
| AUD-007   | Vision                        | Funcional, em estágio inicial; sem necessidade identificada de reescrita imediata               |
| AUD-008   | Control & Commands            | PID básico existente; ciclo de vida de Commands e executor ainda não definidos                  |
| AUD-009   | Utils                         | `MathU` aprovado; `CalcDistAlvo` aprovado com ressalva arquitetural                             |
| AUD-010   | OpModes                       | Problemas operacionais identificados; arquitetura em transição                                  |

### 2.2 Normalização documental

Antes de considerar a documentação encerrada, devem ser corrigidas as inconsistências de identificação:

* O arquivo `AUD-006-Localization.md` termina com uma referência a AUD-005.
* A conclusão de `AUD-007-Vision.md` contém uma identificação textual incorreta.
* `AUD-009-Utils.md` utiliza uma referência a AUD-008 para a própria auditoria de Utils.

A normalização deve corrigir os identificadores e as referências cruzadas sem alterar conclusões técnicas.

---

## 3. Classificação de prioridades

As prioridades deste plano distinguem urgência operacional de dependência arquitetural.

### P0 — Estabilização operacional

Problemas concretos que podem impedir a execução correta ou provocar movimentações inesperadas nos OpModes afetados.

### P1 — Pré-requisitos arquiteturais e correções importantes

Decisões e implementações necessárias para estabelecer contratos consistentes entre subsistemas, controle, localização e execução.

### P2 — Melhorias posteriores

Refatorações, melhorias de testabilidade, limpeza de código e funcionalidades adicionais que não precisam bloquear a estabilização inicial.

A classificação original das auditorias deve ser preservada nos registros. A prioridade de execução pode ser refinada quando uma dependência ou um risco operacional justificar a mudança.

---

## 4. Fase 0 — Estabilização dos OpModes

**Prioridade:** P0
**Dependências:** revisão e aprovação deste plano.
**Objetivo:** eliminar os problemas operacionais concretos identificados na AUD-010 antes de utilizar os OpModes afetados como referência para a implementação da arquitetura.

### 4.1 Corrigir a inicialização da odometria em `NovoTeleop`

A auditoria identificou uma chamada a `odo.init(hardwareMap)` sem que `odo` tenha sido instanciado anteriormente.

Antes de executar esse OpMode, deve-se decidir entre:

* inicializar corretamente a odometria, caso seja necessária para o comportamento pretendido; ou
* remover a dependência, caso a odometria não seja utilizada pelo restante do OpMode.

A decisão não deve antecipar a implementação completa de Localization.

### 4.2 Remover o comportamento experimental de movimento

A auditoria identificou a alternância de `ligado` em todos os ciclos de execução e um movimento de teste associado ao botão `B`.

Esse comportamento deve ser removido do controle operacional normal. Se os movimentos ainda forem necessários para ensaios, deverão ser executados em um contexto de teste explicitamente identificado.

Também deve ser garantido que a lógica experimental não sobrescreva o controle normal do drivetrain.

### 4.3 Resolver a divergência de comportamento de `CalibrarDistanciaRPM`

A documentação e a telemetria afirmam que o drive não será acionado, mas o código apresentado executa o controle do drivetrain.

Deve-se decidir formalmente entre:

* manter a ferramenta sem movimentação do drive; ou
* permitir movimentos limitados de alinhamento, atualizando a documentação, a telemetria e os controles disponíveis.

Até essa decisão, o OpMode não deve ser apresentado ao operador como uma ferramenta que garante que o robô permanecerá parado.

### 4.4 Separar as ações do botão `gamepad2.x`

O botão `X` é utilizado tanto para alinhamento quanto para zerar os encoders do Shooter.

As duas responsabilidades devem ser separadas para evitar que uma única entrada acione operações independentes.

### Critérios de conclusão da Fase 0

* A inicialização de `NovoTeleop` não depende de um objeto nulo.
* O controle normal do drive não é sobrescrito por movimentos experimentais.
* A ferramenta de calibração possui comportamento compatível com sua documentação e telemetria.
* As ações dos botões não apresentam conflitos identificados.
* Os testes dos OpModes afetados foram executados e seus resultados registrados.

Nenhum desses critérios deve ser marcado como concluído antes da validação correspondente.

---

## 5. Fase 1 — Definição da arquitetura-alvo

**Prioridade:** P1
**Dependências:** auditorias individuais completas e estabilização dos riscos operacionais prioritários.

Esta fase estabelece os contratos necessários para implementar a arquitetura sem criar dependências artificiais.

### 5.1 Composição e ciclo de vida do robô

Definir as responsabilidades de:

* `Robot`;
* `HardwareConfig`;
* `RobotConfig`;
* `RConstants`;
* configurações específicas dos subsistemas.

A classe `Robot` deverá ser preenchida somente depois de estabelecido como os componentes serão construídos, inicializados, atualizados e encerrados.

A decisão sobre configuração deve considerar os parâmetros que já existem em `RConstants` e nas classes específicas. Não migrar parâmetros individualmente sem definir um padrão comum.

### 5.2 Contrato de Commands

Definir o ciclo de vida mínimo de um comando.

Uma possibilidade compatível com a auditoria é:

```text
initialize()
    ↓
execute() repetidamente
    ↓
isFinished()
    ↓
end()
```

A assinatura definitiva deverá considerar as necessidades reais dos autônomos e do TeleOp.

Também devem ser definidos:

* como comandos são iniciados e encerrados;
* quem executa os comandos;
* como timeouts são tratados;
* como operações concorrentes são evitadas ou coordenadas;
* como comandos comunicam conclusão e falha;
* como a parada segura é garantida.

Não adotar automaticamente um framework sofisticado. O projeto deve começar com o menor mecanismo de execução que atenda aos requisitos identificados.

### 5.3 Ciclo de atualização dos subsistemas

Definir como os métodos periódicos dos subsistemas serão executados.

Em particular, o `Intake` depende de chamadas regulares a `update()` para finalizar uma operação temporizada.

O contrato escolhido deve evitar que cada OpMode precise implementar individualmente a mesma rotina de atualização.

### 5.4 Contrato de Localization

Antes de ampliar `GobildaOdometry` ou preencher `Localizer`, definir:

* representação de `Pose`;
* unidades e convenções de coordenadas;
* semântica de X, Y e heading;
* origem e orientação do sistema de coordenadas;
* ciclo de inicialização, atualização, reset e consulta;
* responsabilidade pela orientação do robô;
* relação entre IMU e Pinpoint;
* comportamento esperado dos resets.

O restante do software deve consumir uma API de localização, sem depender desnecessariamente do driver específico do Pinpoint.

### 5.5 Fronteira entre Vision e Localization

Preservar a distinção entre:

```text
Vision
    → observações visuais

Localization
    → estimativa de pose

Commands / lógica de decisão
    → escolha da ação

Subsystems
    → atuação física
```

A integração entre Vision e Localization deve ser definida apenas depois que os contratos de ambos estiverem claros.

Não implementar fusão de sensores ou integração direta entre essas camadas sem requisito concreto.

### 5.6 Contrato do PID

Definir:

* como o intervalo de tempo `dt` será fornecido;
* qual é o significado da saída do controlador;
* onde serão aplicados os limites físicos;
* como o estado interno será reiniciado;
* como o comportamento será validado.

O PID deve continuar independente de hardware e de subsistemas.

### Critérios de conclusão da Fase 1

* Responsabilidades das camadas documentadas.
* Contrato de Command e executor mínimo definidos.
* Ciclo de atualização estabelecido.
* Contrato de localização aprovado.
* Relação entre Vision e Localization definida.
* Contrato temporal e de saída do PID documentado.
* Estratégia de configuração aprovada.
* Arquitetura validada antes da implementação.

---

## 6. Fase 2 — Composição e execução

**Prioridade:** P1
**Dependências:** Fase 1 aprovada.

### 6.1 Implementar `Robot`

Implementar a composição dos subsistemas de acordo com as responsabilidades aprovadas.

O contrato deve cobrir:

```text
construção
    ↓
inicialização
    ↓
operação / atualização
    ↓
parada
```

As feature flags existentes devem ser avaliadas nessa camada, caso sejam mantidas.

Não espalhar verificações de configuração global por todos os subsistemas sem necessidade.

### 6.2 Implementar Commands e executor mínimo

Criar a infraestrutura mínima para executar operações de forma previsível e não bloqueante quando o comportamento exigir atualização contínua.

Integrar os comandos aos subsistemas sem duplicar suas responsabilidades.

### 6.3 Integrar o ciclo de atualização

Centralizar as atualizações periódicas necessárias e garantir que as operações temporizadas sejam encerradas corretamente.

### Critérios de conclusão da Fase 2

* Inicialização dos componentes consistente.
* Atualização periódica executada conforme o contrato.
* Comandos iniciados e encerrados corretamente.
* Timeouts e interrupções respeitados.
* Parada segura dos mecanismos implementada.
* Ausência de duplicação desnecessária do ciclo de vida nos OpModes.

---

## 7. Fase 3 — Drivetrain, Localization e controle

**Prioridade:** P1
**Dependências:** contratos de arquitetura aprovados.

### 7.1 Drivetrain

Preservar as responsabilidades já consideradas adequadas:

* controle mecanum;
* controle de heading;
* uso encapsulado da IMU;
* movimentos por encoder;
* calibração de strafe;
* utilização de controlador PID genérico.

Revisar as pendências registradas:

* estado dos motores após movimentos em `RUN_TO_POSITION`;
* configuração da orientação física da IMU;
* parâmetros do PID;
* dependência residual do Pinpoint dentro do Drivetrain;
* contrato de encoder e sua utilização pelos consumidores.

**Comportamento que deve ser preservado:** a auditoria considerou coerente o `isBusy()` do Drivetrain utilizando `OR`, de modo que o movimento permaneça ocupado enquanto qualquer motor relevante ainda estiver ocupado.

Não alterar essa semântica sem evidência técnica e validação.

### 7.2 Localization

Implementar a API de localização conforme o contrato aprovado.

A implementação deverá encapsular o hardware existente, padronizar as unidades e documentar os offsets e as direções dos encoders.

O reset de pose e o reset de IMU devem seguir a decisão arquitetural aprovada, sem presumir que precisam constituir uma única operação.

### 7.3 PIDController

Introduzir `dt` conforme o contrato definido.

Depois, avaliar por testes:

* limites de saída;
* saturação e anti-windup;
* comportamento da derivada;
* alterações abruptas do setpoint;
* estabilidade e tolerância;
* necessidade de recursos adicionais.

Não adicionar filtros, feedforward ou estruturas mais complexas sem uma necessidade demonstrada.

### Critérios de conclusão da Fase 3

* Contratos de pose e heading implementados.
* Movimentos por encoder preservados e testados.
* Configuração de IMU e odometria documentada.
* PID com comportamento temporal definido e testado.
* Ausência de duplicação indevida da inicialização do Pinpoint.
* Resultados de testes técnicos registrados.

---

## 8. Fase 4 — Subsistemas

**Prioridade:** P1/P2, conforme as dependências e os resultados dos testes.

### 8.1 Intake

Manter a implementação funcional enquanto a arquitetura superior é consolidada.

Revisar posteriormente:

* semântica de `isBusy()`;
* estado representado por múltiplos booleanos;
* dependência de `update()`;
* contrato de inicialização;
* organização de `IntakeConfig`;
* abstração de tempo e testabilidade.

Não executar uma grande refatoração do Intake apenas para introduzir uma máquina de estados mais explícita.

O tempo de alimentação de 260 ms deve ser validado fisicamente para confirmar o comportamento esperado de alimentação de uma bola.

### 8.2 Shooter

Manter a estrutura atual, sem reescrita do subsistema.

Antes da calibração formal, confirmar no hardware instalado:

* ticks por revolução do motor;
* RPM nominal;
* relação mecânica entre motor e rolo.

Preservar a condição de prontidão que verifica os dois motores individualmente.

O valor `MANUAL_SHOOTER_RPM = 20.0` deve continuar documentado como configuração emergencial de campo. Não alterá-lo automaticamente para satisfazer o mínimo de 300 RPM. Reavaliar sua finalidade antes da próxima calibração formal.

### 8.3 Viper

A implementação deve permanecer condicionada à definição do mecanismo físico.

Antes de implementar:

1. Documentar a função mecânica.
2. Identificar motores, servos e sensores.
3. Definir nomes e conexões do hardware.
4. Registrar curso, posições e limites físicos.
5. Definir condições de segurança.
6. Estabelecer a API comportamental.
7. Integrar Commands e OpModes.
8. Validar o comportamento em campo.

Não presumir a função do Viper apenas pelo nome do subsistema.

### Critérios de conclusão da Fase 4

* Contratos públicos documentados.
* Responsabilidades entre subsistemas e Commands respeitadas.
* Configurações físicas confirmadas quando necessárias.
* Comportamentos existentes preservados ou mudanças justificadas.
* Testes funcionais executados e registrados.

---

## 9. Fase 5 — Vision e Utils

**Prioridade:** P2, salvo necessidade operacional identificada.

### 9.1 Vision

Preservar a implementação atual de `AprilTagCamera`, que foi considerada funcional e adequadamente encapsulada.

Executar as melhorias registradas:

* normalizar a nomenclatura local;
* documentar a semântica de `hasTarget()`;
* documentar que os getters retornam `0.0` quando não há alvo;
* verificar se os consumidores consultam a presença de alvo antes de utilizar os valores.

Manter `Vision`, `Limelight` e `LimelightConfig` como placeholders enquanto não houver responsabilidade ou requisito concreto.

Não implementar filtros temporais, novas abstrações genéricas ou integração direta com Localization por antecipação.

### 9.2 MathU

Manter `MathU` como utilitário matemático.

Documentar a semântica dos limites e verificar a consistência dos consumidores de normalização angular, sem modificar comportamentos aceitos apenas por preferência de estilo.

### 9.3 CalcDistAlvo

Manter o comportamento atual enquanto não houver evidência de defeito.

Após a consolidação de Vision, Localization e Commands, decidir onde devem residir:

* conversão e calibração de distância;
* geometria entre câmera, AprilTag, alvo e Shooter;
* filtragem temporal;
* cálculo de distância baseado em encoder.

Os parâmetros de calibração física não devem ser alterados sem dados de campo.

### Critérios de conclusão da Fase 5

* Semântica da ausência de alvo documentada e respeitada pelos consumidores.
* Responsabilidades de Vision preservadas.
* Parâmetros físicos mantidos até validação.
* Responsabilidades de `CalcDistAlvo` documentadas e, se necessário, reorganizadas com testes de regressão.

---

## 10. Fase 6 — Evolução dos OpModes

**Prioridade:** P1/P2 após a estabilização e a implementação dos contratos necessários.

### 10.1 AutoBaseSimples

Preservar a direção arquitetural existente.

O tratamento de timeout e a barreira `podeExecutar()` devem ser mantidos, pois ajudam a interromper operações quando o OpMode deixa de estar ativo ou é explicitamente interrompido.

A extração de `atirar()` para Commands deve ocorrer quando o contrato de execução estiver definido, evitando uma refatoração prematura.

### 10.2 TeleopTest2

Utilizar sua separação funcional e o modelo `ShooterMode` como referência para a evolução do TeleOp.

Migrar responsabilidades gradualmente, evitando alterar de uma só vez todo o comportamento operacional.

### 10.3 CalibrarDistanciaRPM

Manter como ferramenta de bancada, separada da arquitetura competitiva.

A ferramenta deve declarar claramente quais mecanismos pode acionar e manter controles, telemetria e documentação coerentes.

### 10.4 NovoTeleop

Tratar como referência histórica e código legado/experimental, não como base arquitetural.

Depois das correções prioritárias, decidir quais comportamentos ainda possuem finalidade operacional e devem ser preservados em uma implementação consolidada.

### 10.5 Consolidação da lógica do Shooter

As diferentes estratégias de controle existentes nos OpModes devem ser revistas para identificar uma única fonte para:

* cálculo de RPM;
* estado do Shooter;
* condição de prontidão;
* sequência de disparo;
* recuperação entre disparos.

Os OpModes devem decidir quando executar as ações; a lógica reutilizável deve ficar na camada definida pela arquitetura aprovada.

### Critérios de conclusão da Fase 6

* Ciclo de vida `init()`, `loop()` e `stop()` consistente.
* Comportamentos operacionais relevantes preservados.
* Lógica de Shooter sem duplicações desnecessárias.
* Sequências de autônomo e TeleOp verificadas em integração.
* Ferramentas de calibração mantidas separadas dos fluxos competitivos.

---

## 11. Fase 7 — Validação final

Nenhuma etapa deve ser considerada concluída somente porque o código foi escrito ou compilou.

A validação deve ocorrer progressivamente:

1. Revisão das alterações.
2. Compilação do projeto.
3. Testes dos componentes aplicáveis.
4. Testes de integração.
5. Testes funcionais controlados.
6. Validação física e calibração, quando necessárias.
7. Registro dos resultados.
8. Aprovação da etapa antes do avanço.

### Áreas que exigem validação física

* parâmetros do motor e transmissão do Shooter;
* tempo de alimentação do Intake;
* offsets e direções dos encoders do Pinpoint;
* orientação da IMU;
* precisão de movimentos por encoder;
* correção de strafe;
* comportamento de visão e distância;
* limites mecânicos e condições de segurança do Viper.

Os resultados deverão ser registrados com o teste executado, a condição de teste, o resultado observado e eventuais pendências.

---

## 12. Matriz consolidada de dependências

| Item                               | Depende de                                         | Resultado esperado                       |
| ---------------------------------- | -------------------------------------------------- | ---------------------------------------- |
| Correções operacionais dos OpModes | Aprovação do plano                                 | Fluxos afetados estabilizados            |
| `Robot`                            | Contratos de inicialização, atualização e parada   | Composição central consistente           |
| Commands / executor                | Ciclo de vida e semântica de execução              | Coordenação previsível                   |
| Intake                             | Ciclo de atualização e contrato de API             | Alimentação temporizada confiável        |
| Localization                       | Pose, unidades, heading e ciclo de vida            | API consumível pelo restante do software |
| Drivetrain                         | Contrato de movimento e fronteira com Localization | Movimento e orientação consistentes      |
| PIDController                      | Contrato temporal e de saída                       | Controle discreto testável               |
| Vision                             | Semântica da ausência de alvo                      | Observações consumidas com segurança     |
| CalcDistAlvo                       | Decisões de Vision, Localization e Shooter         | Responsabilidades claras                 |
| Viper                              | Requisitos mecânicos e hardware                    | Implementação funcional verificável      |
| OpModes                            | Subsistemas e Commands estáveis                    | Fluxos de operação integrados            |

---

## 13. Decisões que exigem validação explícita

Antes de iniciar a implementação, devem ser aprovadas:

* [ ] Prioridade e escopo das correções P0 dos OpModes.
* [ ] Responsabilidades de `Robot` e estratégia de configuração.
* [ ] Ciclo de vida de Commands e executor mínimo.
* [ ] Estratégia de atualização dos subsistemas.
* [ ] Contrato de localização, pose, heading e reset.
* [ ] Fronteira entre Drivetrain, IMU e Pinpoint.
* [ ] Contrato temporal e de saída do PID.
* [ ] Preservação dos comportamentos aprovados em Drivetrain, Intake e Shooter.
* [ ] Pré-requisitos mecânicos e de hardware do Viper.
* [ ] Sequência de integração de Vision, Utils e OpModes.
* [ ] Critérios de validação de cada fase.

As decisões que não forem aprovadas deverão permanecer registradas como pendências, sem serem convertidas em implementação por suposição.

---

## 14. Critérios de encerramento da consolidação

A consolidação será considerada concluída quando:

1. As dez auditorias estiverem identificadas e referenciadas corretamente.
2. Os achados cruzados estiverem registrados.
3. Problemas confirmados estiverem separados de hipóteses e melhorias opcionais.
4. Dependências arquiteturais estiverem documentadas.
5. A sequência de implementação estiver aprovada.
6. Os critérios de validação estiverem definidos.
7. O responsável pelo projeto aprovar formalmente este plano.

**Estado atual:** proposta apresentada para validação.

**Código Java alterado nesta etapa:** não.

**Próxima ação:** revisar e aprovar o plano antes de iniciar qualquer modificação de código.
