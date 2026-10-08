# AUD-006 — Vision

**Projeto:** MannaTeam FTC 31211 — 2026
**Área:** `vision`
**Status da auditoria:** Concluída
**Escopo:** `Vision`, `AprilTagCamera`, `Limelight`, `LimelightConfig`

---

## 1. Objetivo da auditoria

Avaliar a camada de visão atualmente existente no projeto, verificando:

* organização das responsabilidades;
* implementação da câmera AprilTag;
* ciclo de inicialização, atualização e encerramento;
* tratamento de ausência de detecção;
* seleção do alvo;
* dependências com `RConstants`;
* qualidade e consistência da API exposta aos demais componentes;
* preparação da arquitetura para eventual uso de Limelight;
* integração futura com Localization, Commands e Subsystems.

A auditoria considera o código apresentado como **estado atual do projeto**, e não como arquitetura definitiva.

---

# 2. Estrutura atual

A estrutura apresentada é:

```text
vision/
├── Vision.java
├── apriltag/
│   └── AprilTagCamera.java
└── limelight/
    ├── Limelight.java
    └── LimelightConfig.java
```

### Estado das classes

| Classe            | Estado    | Função atual                           |
| ----------------- | --------- | -------------------------------------- |
| `Vision`          | vazia     | sem responsabilidade implementada      |
| `AprilTagCamera`  | funcional | captura e seleciona detecções AprilTag |
| `Limelight`       | vazia     | reservada para futura implementação    |
| `LimelightConfig` | vazia     | reservada para futura configuração     |

Portanto, atualmente a única implementação efetiva da camada é `AprilTagCamera`.

---

# 3. `AprilTagCamera`

## 3.1. Inicialização

A câmera é inicializada por:

```java
public void init(HardwareMap hardwareMap)
```

O método cria um `AprilTagProcessor` e um `VisionPortal`.

A configuração atual é:

```java
.setDrawAxes(true)
.setDrawCubeProjection(true)
.setDrawTagID(true)
.setDrawTagOutline(true)
```

e:

```java
.setCamera(hardwareMap.get(WebcamName.class, RConstants.WEBCAM))
.setCameraResolution(new Size(640, 480))
.addProcessor(aprilTagProcessor)
.build();
```

### Avaliação

A implementação é objetiva e suficiente para colocar a detecção AprilTag em funcionamento.

Não há, entretanto, separação entre:

* configuração da câmera;
* configuração do detector;
* resolução;
* parâmetros específicos do pipeline;
* identificação da câmera.

Tudo está atualmente concentrado em `AprilTagCamera`.

Isso funciona no estágio atual, mas tende a dificultar manutenção quando a visão crescer.

---

# 4. Ciclo de vida

A classe possui três operações principais:

```text
init()
update()
close()
```

Isso é uma boa base conceitual para o sistema.

O `close()` também foi implementado de forma adequada:

```java
if (visionPortal != null) {
    visionPortal.close();
    visionPortal = null;
}

aprilTagProcessor = null;
atualDetection = null;
```

Isso evita deixar explicitamente o `VisionPortal` aberto quando o componente deixa de ser utilizado.

### Ponto positivo

Existe uma preocupação explícita com o ciclo de vida do recurso de câmera.

### Ponto a melhorar

Não existe atualmente um estado explícito indicando:

```text
não inicializado
inicializado
ativo
encerrado
```

Assim, a classe depende implicitamente de `null` para representar seu estado.

Para o projeto atual isso não é necessariamente um problema, mas deve ser considerado caso a camada de visão passe a ser utilizada por múltiplos componentes.

---

# 5. Atualização das detecções

O método:

```java
public void update()
```

começa com:

```java
atualDetection = null;
```

Essa decisão é importante.

A cada ciclo, a classe descarta a detecção anterior e reconstrói o estado a partir das detecções atuais.

Isso significa que:

> **a classe não mantém automaticamente uma detecção antiga como alvo válido.**

Esse comportamento é geralmente desejável para uma camada de percepção.

Caso nenhum AprilTag válido seja encontrado:

```java
atualDetection = null;
```

permanece.

Consequentemente:

```java
hasTarget()
```

retorna `false`.

---

# 6. Seleção do alvo

A seleção atual é:

```java
AprilTagDetection DetectionProxima = null;
double RangeProximo = Double.MAX_VALUE;
```

seguida de:

```java
for (AprilTagDetection detection : detections) {
```

com os filtros:

```java
if (detection.metadata == null || detection.ftcPose == null) {
    continue;
}
```

e:

```java
if (RConstants.TARGET_APRIL_TAG_ID >= 0
        && detection.id != RConstants.TARGET_APRIL_TAG_ID) {
    continue;
}
```

Por fim, entre as detecções válidas, é escolhida aquela com menor:

```java
detection.ftcPose.range
```

---

## 6.1. Comportamento resultante

A lógica atual pode ser descrita como:

```text
Todas as detecções
       │
       ▼
metadata válido?
       │
       ├── não → descarta
       │
       ▼
ftcPose válido?
       │
       ├── não → descarta
       │
       ▼
TARGET_APRIL_TAG_ID configurado?
       │
       ├── sim → aceita somente esse ID
       │
       └── não → aceita qualquer ID
       │
       ▼
menor range
       │
       ▼
alvo atual
```

Essa é uma política de seleção clara e determinística.

---

# 7. Dependência com `RConstants`

A câmera depende diretamente de:

```java
RConstants.WEBCAM
```

e:

```java
RConstants.TARGET_APRIL_TAG_ID
```

Isso é coerente com o padrão atual do projeto, onde `RConstants` concentra parâmetros globais do robô.

Entretanto, existe uma diferença importante entre:

### Configuração do robô

Exemplo:

```java
RConstants.WEBCAM
```

e

### Política de visão

Exemplo:

```java
RConstants.TARGET_APRIL_TAG_ID
```

A segunda não é apenas uma constante de hardware. Ela representa uma **regra de seleção de alvo**.

No futuro, pode ser interessante separar:

```text
Hardware configuration
        +
Vision configuration
        +
Runtime target selection
```

Não é uma correção obrigatória neste momento.

---

# 8. API de leitura

A classe expõe:

```java
hasTarget()
getTargetId()
getRangeInches()
getBearingDegrees()
getYawDegrees()
getForwardInches()
getSideInches()
```

Essa API é simples e adequada para consumo externo.

Um componente superior pode fazer:

```java
if (camera.hasTarget()) {
    double range = camera.getRangeInches();
    double bearing = camera.getBearingDegrees();
}
```

sem precisar conhecer:

```java
AprilTagDetection
AprilTagProcessor
VisionPortal
```

### Esse é um ponto arquitetural positivo.

A implementação da FTC Vision API está encapsulada dentro de `AprilTagCamera`.

---

# 9. Problema importante: semântica dos valores inválidos

Atualmente, quando não existe alvo:

```java
getRangeInches()    → 0.0
getBearingDegrees() → 0.0
getYawDegrees()     → 0.0
getForwardInches()  → 0.0
getSideInches()     → 0.0
```

Isso merece atenção.

Por exemplo:

```java
double range = camera.getRangeInches();
```

não permite distinguir:

```text
range = 0
```

de:

```text
não existe alvo
```

Embora `hasTarget()` permita fazer essa distinção corretamente, a API cria uma possibilidade de uso incorreto.

### Recomendação

Manter `hasTarget()` como mecanismo principal de validade.

Em uma futura evolução, considerar uma API baseada em um objeto de resultado, por exemplo conceitualmente:

```text
VisionTarget
    id
    range
    bearing
    yaw
    forward
    side
```

Assim:

```text
resultado válido
ou
nenhum resultado
```

ficaria representado explicitamente.

**Não recomendo fazer essa alteração apenas por estética agora.**

Ela deve ser feita quando houver necessidade real de evoluir a API.

---

# 10. Ausência de filtragem temporal

O código atual utiliza somente a detecção do frame atual.

Não existe:

* média móvel;
* filtro temporal;
* confirmação de múltiplos frames;
* timeout;
* persistência controlada;
* rejeição de outliers.

Isso não é necessariamente um erro.

Na verdade, é importante não introduzir filtragem prematuramente.

A necessidade de filtragem deve surgir de um problema observado em campo, por exemplo:

```text
detecção instável
       ↓
range oscila
       ↓
comando oscila
       ↓
movimento do robô fica instável
```

Caso isso aconteça, a filtragem deve ser adicionada preferencialmente **entre percepção e decisão**, e não necessariamente dentro do detector bruto.

---

# 11. `AprilTagCamera` não deve assumir responsabilidades de Localization

Este ponto é especialmente importante considerando a auditoria da camada de Localization.

A câmera atualmente fornece:

```text
range
bearing
yaw
x
y
```

Esses valores representam a relação entre câmera/tag segundo o sistema de coordenadas fornecido pelo `ftcPose`.

Isso **não transforma `AprilTagCamera` em um sistema de localização global do robô**.

A arquitetura deve preservar a distinção:

```text
Vision
   │
   └── observa elementos do ambiente
          │
          ▼
Localization
   │
   └── estima pose do robô
```

Portanto, não recomendo mover lógica de pose global para `AprilTagCamera` apenas porque AprilTags podem ser utilizados para localização.

---

# 12. Integração futura com Localization

Existe uma oportunidade arquitetural importante.

No futuro, a informação AprilTag poderá alimentar Localization:

```text
AprilTagCamera
      │
      │ observação
      ▼
Localization
      │
      │ pose estimada
      ▼
Drive / Commands
```

Mas essa integração deve ser feita de maneira explícita.

A câmera não deve passar a conhecer:

* odometria;
* drive;
* comandos;
* trajetória;
* estado do robô.

Isso manterá a separação de responsabilidades.

---

# 13. `Vision`

Atualmente:

```java
public class Vision {
}
```

A classe está completamente vazia.

### Avaliação

Não há necessidade de preenchê-la automaticamente.

Antes de criar uma classe agregadora, deve ser definida sua responsabilidade.

Existem pelo menos duas possibilidades:

### Opção A — Fachada

```text
Vision
 ├── AprilTagCamera
 └── Limelight
```

Nesse modelo, `Vision` seria o ponto de acesso principal à percepção.

### Opção B — Não existir

Os consumidores utilizariam diretamente:

```text
AprilTagCamera
Limelight
```

A existência de `Vision` somente para agrupar classes sem responsabilidade própria não agrega valor.

### Recomendação

**Não implementar `Vision` ainda.**

Primeiro deve ser definido como o projeto realmente utilizará múltiplas fontes de visão.

---

# 14. Limelight

Atualmente:

```java
public class Limelight {
}
```

e:

```java
public class LimelightConfig {
}
```

estão vazias.

Isso deve ser tratado como **estrutura preparada para expansão**, não como falha funcional.

Não existe evidência suficiente no código fornecido para determinar:

* se haverá Limelight fisicamente no robô;
* qual modelo;
* quais pipelines;
* se será utilizada para AprilTags;
* se será utilizada para localização;
* se substituirá ou complementará a webcam;
* quais dados serão consumidos.

Portanto, não é recomendável implementar uma abstração genérica prematuramente.

---

# 15. Nomenclatura

Existem algumas inconsistências de estilo:

```java
AprilTagDetection DetectionProxima = null;
double RangeProximo = Double.MAX_VALUE;
```

Pelas convenções Java utilizadas no restante do projeto, seriam mais adequados:

```java
AprilTagDetection detectionProxima = null;
double rangeProximo = Double.MAX_VALUE;
```

Classes:

```text
Vision
AprilTagCamera
Limelight
LimelightConfig
```

estão corretamente em `PascalCase`.

Métodos:

```text
init()
update()
hasTarget()
getTargetId()
```

também seguem a convenção esperada.

### Classificação

**Problema de baixa severidade.**

Não afeta comportamento.

---

# 16. Capitalização de `atualDetection`

O campo:

```java
private AprilTagDetection atualDetection;
```

está semanticamente compreensível.

Entretanto, como o projeto está escrito predominantemente em inglês, seria mais consistente utilizar:

```java
currentDetection
```

ou, melhor ainda, caso o campo represente especificamente o alvo selecionado:

```java
currentTarget
```

Isso é apenas uma questão de consistência arquitetural/nomenclatura.

Não deve ser priorizado antes de mudanças funcionais.

---

# 17. Tratamento de inicialização

Existe uma proteção:

```java
if (aprilTagProcessor == null) {
    return;
}
```

Isso evita erro em:

```java
update()
```

caso `init()` ainda não tenha sido chamado.

Entretanto, os getters simplesmente retornam valores padrão.

Isso cria um comportamento silencioso.

Para o uso em competição, esse comportamento pode ser desejável em alguns cenários, pois evita derrubar o OpMode por uma consulta inválida.

Por outro lado, durante desenvolvimento, erros de ciclo de vida podem ficar escondidos.

### Recomendação

Manter o comportamento tolerante por enquanto.

Se o projeto posteriormente adotar uma infraestrutura de diagnóstico/logging, esse estado pode gerar um aviso de inicialização incorreta.

---

# 18. Performance

A configuração atual utiliza:

```java
new Size(640, 480)
```

Essa resolução é relativamente moderada e pode ser uma escolha razoável para visão embarcada.

Entretanto, a resolução deve ser considerada junto com:

* FPS;
* distância dos AprilTags;
* tamanho físico dos tags;
* iluminação;
* CPU disponível;
* latência desejada.

A auditoria não possui dados de campo suficientes para determinar que `640x480` deve ser alterado.

Portanto:

**não há recomendação de mudança de resolução neste momento.**

---

# 19. Desenho atual de responsabilidades

A arquitetura atual pode ser representada como:

```text
                    VISION
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼                       ▼
   AprilTagCamera             Limelight
          │                       │
          │                       │
          ▼                       ▼
   FTC Vision API             Futuro
```

Isso é aceitável como ponto de partida.

O que deve ser evitado futuramente:

```text
AprilTagCamera
      │
      ├── Drive
      ├── Shooter
      ├── Localization
      ├── Commands
      └── Robot state
```

A câmera deve permanecer uma fonte de percepção.

---

# 20. Integração com Commands

A camada de visão deve fornecer informações.

Por exemplo:

```text
Vision
  ↓
"Tag 5 está a 30 in, bearing -4°"
  ↓
Command
  ↓
decide o que fazer
```

Não:

```text
AprilTagCamera
  ↓
move drivetrain
```

Isso mantém a separação:

```text
Vision       → percepção
Localization → estado/pose
Commands     → decisão
Subsystems   → atuação
```

Essa separação é especialmente importante para a evolução futura do projeto.

---

# 21. Integração com Shooter

O `AprilTagCamera` não deve conhecer o Shooter.

Um fluxo futuro aceitável seria:

```text
AprilTagCamera
       │
       │ range
       ▼
CalcDistAlvo / camada de decisão
       │
       │ distância calculada
       ▼
Shooter Command
       │
       ▼
Shooter
```

Isso mantém a câmera independente do mecanismo que utiliza seus dados.

---

# 22. Achados classificados

### 🔴 Críticos

**Nenhum identificado no código apresentado.**

---

### 🟠 Importantes

**VISION-001 — API representa ausência de alvo como valores numéricos válidos**

Os getters retornam `0.0` quando não há detecção.

Isso pode causar confusão caso um consumidor esqueça de consultar `hasTarget()`.

**Ação:** considerar uma API de resultado explícito em uma futura evolução.

---

**VISION-002 — Política de seleção de alvo está embutida na câmera**

A câmera decide automaticamente:

```text
TARGET_APRIL_TAG_ID
        +
menor range
```

Isso é funcional, mas mistura percepção com uma política específica de seleção.

**Ação:** manter por enquanto; reavaliar quando houver necessidade de múltiplas políticas de alvo.

---

### 🟡 Melhorias

**VISION-003 — Nomenclatura inconsistente**

```java
DetectionProxima
RangeProximo
```

deveriam seguir a convenção Java:

```java
detectionProxima
rangeProximo
```

---

**VISION-004 — Configuração de visão está parcialmente misturada com configuração geral**

`WEBCAM` e `TARGET_APRIL_TAG_ID` estão em `RConstants`.

**Ação:** não alterar imediatamente; considerar uma futura `VisionConfig` quando a camada crescer.

---

**VISION-005 — `Vision` ainda não possui responsabilidade definida**

Não deve ser preenchida apenas para criar uma abstração artificial.

---

**VISION-006 — Limelight ainda não possui implementação**

Não constitui problema enquanto o hardware/uso não estiver definido.

---

### 🟢 Pontos positivos

**VISION-007 — Boa encapsulação da API FTC Vision**

Os consumidores não precisam acessar diretamente `AprilTagProcessor` ou `VisionPortal`.

---

**VISION-008 — Ciclo de vida explícito**

Existem operações claras de:

```text
init()
update()
close()
```

---

**VISION-009 — Detecção antiga não é mantida indevidamente**

Cada `update()` começa com:

```java
atualDetection = null;
```

evitando tratar uma detecção antiga como atual.

---

**VISION-010 — Filtragem de detecções inválidas**

São descartadas detecções sem:

```java
metadata
ftcPose
```

---

**VISION-011 — Seleção determinística**

Quando múltiplos alvos são válidos, a implementação seleciona o menor `range`.

---

# 23. Decisões recomendadas

## Fazer agora

1. Corrigir apenas a nomenclatura local:

   ```java
   DetectionProxima → detectionProxima
   RangeProximo → rangeProximo
   ```

2. Documentar a semântica de:

   ```java
   hasTarget()
   ```

3. Documentar que os getters retornam `0.0` quando não há alvo.

4. Manter `Vision` vazia até existir uma responsabilidade concreta.

5. Manter `Limelight` e `LimelightConfig` como placeholders até a definição do hardware e do uso.

---

## Não fazer agora

Não recomendo, neste estágio:

* criar uma abstração genérica `VisionTarget` apenas por antecipação;
* criar uma interface `VisionProvider`;
* integrar AprilTag diretamente com Localization;
* fazer `AprilTagCamera` controlar o Drive;
* mover lógica para Commands;
* implementar Limelight sem requisito concreto;
* adicionar filtros temporais sem evidência de instabilidade;
* alterar resolução sem testes de campo.

Essas mudanças aumentariam a complexidade sem evidência de que resolvem um problema atual.

---

# 24. Arquitetura-alvo sugerida

Uma evolução natural seria:

```text
                         Vision
                           │
             ┌─────────────┴─────────────┐
             │                           │
             ▼                           ▼
      AprilTagCamera                Limelight
             │                           │
             └─────────────┬─────────────┘
                           │
                           ▼
                    Vision observations
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
        Localization              Commands / Logic
              │                         │
              ▼                         ▼
        Robot Pose                 Robot actions
```

A ideia principal é que **Vision produza observações**, enquanto outras camadas decidam como utilizá-las.

---

# 25. Conclusão da AUD-006

A camada de Vision está em um estado **funcional, porém inicial**.

A implementação de `AprilTagCamera` já fornece uma abstração razoavelmente limpa sobre a API FTC Vision e apresenta um ciclo de vida coerente. Não foram identificados problemas críticos ou falhas estruturais que exijam uma reescrita.

O principal ponto arquitetural para as próximas auditorias é preservar a separação:

```text
Vision
  ↓
observação

Localization
  ↓
estimativa de pose

Commands
  ↓
decisão

Subsystems
  ↓
atuação
```

A existência de `Vision`, `Limelight` e `LimelightConfig` vazias deve ser entendida como **espaço reservado para evolução**, e não como algo que precise ser preenchido imediatamente.

### Estado final

| Item                                | Avaliação                          |
| ----------------------------------- | ---------------------------------- |
| `AprilTagCamera`                    | 🟢 Funcional                       |
| Ciclo de vida                       | 🟢 Adequado                        |
| Seleção de alvo                     | 🟢 Funcional                       |
| Encapsulamento FTC Vision           | 🟢 Bom                             |
| Tratamento de ausência de alvo      | 🟡 Melhorável                      |
| Configuração                        | 🟡 Pode evoluir                    |
| `Vision`                            | ⚪ Placeholder                      |
| `Limelight`                         | ⚪ Placeholder                      |
| Integração com Localization         | ⚪ Deve ser definida posteriormente |
| Necessidade de refatoração imediata | **Baixa**                          |
| Prioridade geral                    | **Baixa/Média**                    |

**AUD-007-Vision: APROVADA COM MELHORIAS FUTURAS.**
# AUD-007 — Vision

**Projeto:** MannaTeam FTC 31211 — 2026
**Área:** `vision`
**Status da auditoria:** Concluída
**Escopo:** `Vision`, `AprilTagCamera`, `Limelight`, `LimelightConfig`

---

## 1. Objetivo da auditoria

Avaliar a camada de visão atualmente existente no projeto, verificando:

* organização das responsabilidades;
* implementação da câmera AprilTag;
* ciclo de inicialização, atualização e encerramento;
* tratamento de ausência de detecção;
* seleção do alvo;
* dependências com `RConstants`;
* qualidade e consistência da API exposta aos demais componentes;
* preparação da arquitetura para eventual uso de Limelight;
* integração futura com Localization, Commands e Subsystems.

A auditoria considera o código apresentado como **estado atual do projeto**, e não como arquitetura definitiva.

---

# 2. Estrutura atual

A estrutura apresentada é:

```text
vision/
├── Vision.java
├── apriltag/
│   └── AprilTagCamera.java
└── limelight/
    ├── Limelight.java
    └── LimelightConfig.java
```

### Estado das classes

| Classe            | Estado    | Função atual                           |
| ----------------- | --------- | -------------------------------------- |
| `Vision`          | vazia     | sem responsabilidade implementada      |
| `AprilTagCamera`  | funcional | captura e seleciona detecções AprilTag |
| `Limelight`       | vazia     | reservada para futura implementação    |
| `LimelightConfig` | vazia     | reservada para futura configuração     |

Portanto, atualmente a única implementação efetiva da camada é `AprilTagCamera`.

---

# 3. `AprilTagCamera`

## 3.1. Inicialização

A câmera é inicializada por:

```java
public void init(HardwareMap hardwareMap)
```

O método cria um `AprilTagProcessor` e um `VisionPortal`.

A configuração atual é:

```java
.setDrawAxes(true)
.setDrawCubeProjection(true)
.setDrawTagID(true)
.setDrawTagOutline(true)
```

e:

```java
.setCamera(hardwareMap.get(WebcamName.class, RConstants.WEBCAM))
.setCameraResolution(new Size(640, 480))
.addProcessor(aprilTagProcessor)
.build();
```

### Avaliação

A implementação é objetiva e suficiente para colocar a detecção AprilTag em funcionamento.

Não há, entretanto, separação entre:

* configuração da câmera;
* configuração do detector;
* resolução;
* parâmetros específicos do pipeline;
* identificação da câmera.

Tudo está atualmente concentrado em `AprilTagCamera`.

Isso funciona no estágio atual, mas tende a dificultar manutenção quando a visão crescer.

---

# 4. Ciclo de vida

A classe possui três operações principais:

```text
init()
update()
close()
```

Isso é uma boa base conceitual para o sistema.

O `close()` também foi implementado de forma adequada:

```java
if (visionPortal != null) {
    visionPortal.close();
    visionPortal = null;
}

aprilTagProcessor = null;
atualDetection = null;
```

Isso evita deixar explicitamente o `VisionPortal` aberto quando o componente deixa de ser utilizado.

### Ponto positivo

Existe uma preocupação explícita com o ciclo de vida do recurso de câmera.

### Ponto a melhorar

Não existe atualmente um estado explícito indicando:

```text
não inicializado
inicializado
ativo
encerrado
```

Assim, a classe depende implicitamente de `null` para representar seu estado.

Para o projeto atual isso não é necessariamente um problema, mas deve ser considerado caso a camada de visão passe a ser utilizada por múltiplos componentes.

---

# 5. Atualização das detecções

O método:

```java
public void update()
```

começa com:

```java
atualDetection = null;
```

Essa decisão é importante.

A cada ciclo, a classe descarta a detecção anterior e reconstrói o estado a partir das detecções atuais.

Isso significa que:

> **a classe não mantém automaticamente uma detecção antiga como alvo válido.**

Esse comportamento é geralmente desejável para uma camada de percepção.

Caso nenhum AprilTag válido seja encontrado:

```java
atualDetection = null;
```

permanece.

Consequentemente:

```java
hasTarget()
```

retorna `false`.

---

# 6. Seleção do alvo

A seleção atual é:

```java
AprilTagDetection DetectionProxima = null;
double RangeProximo = Double.MAX_VALUE;
```

seguida de:

```java
for (AprilTagDetection detection : detections) {
```

com os filtros:

```java
if (detection.metadata == null || detection.ftcPose == null) {
    continue;
}
```

e:

```java
if (RConstants.TARGET_APRIL_TAG_ID >= 0
        && detection.id != RConstants.TARGET_APRIL_TAG_ID) {
    continue;
}
```

Por fim, entre as detecções válidas, é escolhida aquela com menor:

```java
detection.ftcPose.range
```

---

## 6.1. Comportamento resultante

A lógica atual pode ser descrita como:

```text
Todas as detecções
       │
       ▼
metadata válido?
       │
       ├── não → descarta
       │
       ▼
ftcPose válido?
       │
       ├── não → descarta
       │
       ▼
TARGET_APRIL_TAG_ID configurado?
       │
       ├── sim → aceita somente esse ID
       │
       └── não → aceita qualquer ID
       │
       ▼
menor range
       │
       ▼
alvo atual
```

Essa é uma política de seleção clara e determinística.

---

# 7. Dependência com `RConstants`

A câmera depende diretamente de:

```java
RConstants.WEBCAM
```

e:

```java
RConstants.TARGET_APRIL_TAG_ID
```

Isso é coerente com o padrão atual do projeto, onde `RConstants` concentra parâmetros globais do robô.

Entretanto, existe uma diferença importante entre:

### Configuração do robô

Exemplo:

```java
RConstants.WEBCAM
```

e

### Política de visão

Exemplo:

```java
RConstants.TARGET_APRIL_TAG_ID
```

A segunda não é apenas uma constante de hardware. Ela representa uma **regra de seleção de alvo**.

No futuro, pode ser interessante separar:

```text
Hardware configuration
        +
Vision configuration
        +
Runtime target selection
```

Não é uma correção obrigatória neste momento.

---

# 8. API de leitura

A classe expõe:

```java
hasTarget()
getTargetId()
getRangeInches()
getBearingDegrees()
getYawDegrees()
getForwardInches()
getSideInches()
```

Essa API é simples e adequada para consumo externo.

Um componente superior pode fazer:

```java
if (camera.hasTarget()) {
    double range = camera.getRangeInches();
    double bearing = camera.getBearingDegrees();
}
```

sem precisar conhecer:

```java
AprilTagDetection
AprilTagProcessor
VisionPortal
```

### Esse é um ponto arquitetural positivo.

A implementação da FTC Vision API está encapsulada dentro de `AprilTagCamera`.

---

# 9. Problema importante: semântica dos valores inválidos

Atualmente, quando não existe alvo:

```java
getRangeInches()    → 0.0
getBearingDegrees() → 0.0
getYawDegrees()     → 0.0
getForwardInches()  → 0.0
getSideInches()     → 0.0
```

Isso merece atenção.

Por exemplo:

```java
double range = camera.getRangeInches();
```

não permite distinguir:

```text
range = 0
```

de:

```text
não existe alvo
```

Embora `hasTarget()` permita fazer essa distinção corretamente, a API cria uma possibilidade de uso incorreto.

### Recomendação

Manter `hasTarget()` como mecanismo principal de validade.

Em uma futura evolução, considerar uma API baseada em um objeto de resultado, por exemplo conceitualmente:

```text
VisionTarget
    id
    range
    bearing
    yaw
    forward
    side
```

Assim:

```text
resultado válido
ou
nenhum resultado
```

ficaria representado explicitamente.

**Não recomendo fazer essa alteração apenas por estética agora.**

Ela deve ser feita quando houver necessidade real de evoluir a API.

---

# 10. Ausência de filtragem temporal

O código atual utiliza somente a detecção do frame atual.

Não existe:

* média móvel;
* filtro temporal;
* confirmação de múltiplos frames;
* timeout;
* persistência controlada;
* rejeição de outliers.

Isso não é necessariamente um erro.

Na verdade, é importante não introduzir filtragem prematuramente.

A necessidade de filtragem deve surgir de um problema observado em campo, por exemplo:

```text
detecção instável
       ↓
range oscila
       ↓
comando oscila
       ↓
movimento do robô fica instável
```

Caso isso aconteça, a filtragem deve ser adicionada preferencialmente **entre percepção e decisão**, e não necessariamente dentro do detector bruto.

---

# 11. `AprilTagCamera` não deve assumir responsabilidades de Localization

Este ponto é especialmente importante considerando a auditoria da camada de Localization.

A câmera atualmente fornece:

```text
range
bearing
yaw
x
y
```

Esses valores representam a relação entre câmera/tag segundo o sistema de coordenadas fornecido pelo `ftcPose`.

Isso **não transforma `AprilTagCamera` em um sistema de localização global do robô**.

A arquitetura deve preservar a distinção:

```text
Vision
   │
   └── observa elementos do ambiente
          │
          ▼
Localization
   │
   └── estima pose do robô
```

Portanto, não recomendo mover lógica de pose global para `AprilTagCamera` apenas porque AprilTags podem ser utilizados para localização.

---

# 12. Integração futura com Localization

Existe uma oportunidade arquitetural importante.

No futuro, a informação AprilTag poderá alimentar Localization:

```text
AprilTagCamera
      │
      │ observação
      ▼
Localization
      │
      │ pose estimada
      ▼
Drive / Commands
```

Mas essa integração deve ser feita de maneira explícita.

A câmera não deve passar a conhecer:

* odometria;
* drive;
* comandos;
* trajetória;
* estado do robô.

Isso manterá a separação de responsabilidades.

---

# 13. `Vision`

Atualmente:

```java
public class Vision {
}
```

A classe está completamente vazia.

### Avaliação

Não há necessidade de preenchê-la automaticamente.

Antes de criar uma classe agregadora, deve ser definida sua responsabilidade.

Existem pelo menos duas possibilidades:

### Opção A — Fachada

```text
Vision
 ├── AprilTagCamera
 └── Limelight
```

Nesse modelo, `Vision` seria o ponto de acesso principal à percepção.

### Opção B — Não existir

Os consumidores utilizariam diretamente:

```text
AprilTagCamera
Limelight
```

A existência de `Vision` somente para agrupar classes sem responsabilidade própria não agrega valor.

### Recomendação

**Não implementar `Vision` ainda.**

Primeiro deve ser definido como o projeto realmente utilizará múltiplas fontes de visão.

---

# 14. Limelight

Atualmente:

```java
public class Limelight {
}
```

e:

```java
public class LimelightConfig {
}
```

estão vazias.

Isso deve ser tratado como **estrutura preparada para expansão**, não como falha funcional.

Não existe evidência suficiente no código fornecido para determinar:

* se haverá Limelight fisicamente no robô;
* qual modelo;
* quais pipelines;
* se será utilizada para AprilTags;
* se será utilizada para localização;
* se substituirá ou complementará a webcam;
* quais dados serão consumidos.

Portanto, não é recomendável implementar uma abstração genérica prematuramente.

---

# 15. Nomenclatura

Existem algumas inconsistências de estilo:

```java
AprilTagDetection DetectionProxima = null;
double RangeProximo = Double.MAX_VALUE;
```

Pelas convenções Java utilizadas no restante do projeto, seriam mais adequados:

```java
AprilTagDetection detectionProxima = null;
double rangeProximo = Double.MAX_VALUE;
```

Classes:

```text
Vision
AprilTagCamera
Limelight
LimelightConfig
```

estão corretamente em `PascalCase`.

Métodos:

```text
init()
update()
hasTarget()
getTargetId()
```

também seguem a convenção esperada.

### Classificação

**Problema de baixa severidade.**

Não afeta comportamento.

---

# 16. Capitalização de `atualDetection`

O campo:

```java
private AprilTagDetection atualDetection;
```

está semanticamente compreensível.

Entretanto, como o projeto está escrito predominantemente em inglês, seria mais consistente utilizar:

```java
currentDetection
```

ou, melhor ainda, caso o campo represente especificamente o alvo selecionado:

```java
currentTarget
```

Isso é apenas uma questão de consistência arquitetural/nomenclatura.

Não deve ser priorizado antes de mudanças funcionais.

---

# 17. Tratamento de inicialização

Existe uma proteção:

```java
if (aprilTagProcessor == null) {
    return;
}
```

Isso evita erro em:

```java
update()
```

caso `init()` ainda não tenha sido chamado.

Entretanto, os getters simplesmente retornam valores padrão.

Isso cria um comportamento silencioso.

Para o uso em competição, esse comportamento pode ser desejável em alguns cenários, pois evita derrubar o OpMode por uma consulta inválida.

Por outro lado, durante desenvolvimento, erros de ciclo de vida podem ficar escondidos.

### Recomendação

Manter o comportamento tolerante por enquanto.

Se o projeto posteriormente adotar uma infraestrutura de diagnóstico/logging, esse estado pode gerar um aviso de inicialização incorreta.

---

# 18. Performance

A configuração atual utiliza:

```java
new Size(640, 480)
```

Essa resolução é relativamente moderada e pode ser uma escolha razoável para visão embarcada.

Entretanto, a resolução deve ser considerada junto com:

* FPS;
* distância dos AprilTags;
* tamanho físico dos tags;
* iluminação;
* CPU disponível;
* latência desejada.

A auditoria não possui dados de campo suficientes para determinar que `640x480` deve ser alterado.

Portanto:

**não há recomendação de mudança de resolução neste momento.**

---

# 19. Desenho atual de responsabilidades

A arquitetura atual pode ser representada como:

```text
                    VISION
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼                       ▼
   AprilTagCamera             Limelight
          │                       │
          │                       │
          ▼                       ▼
   FTC Vision API             Futuro
```

Isso é aceitável como ponto de partida.

O que deve ser evitado futuramente:

```text
AprilTagCamera
      │
      ├── Drive
      ├── Shooter
      ├── Localization
      ├── Commands
      └── Robot state
```

A câmera deve permanecer uma fonte de percepção.

---

# 20. Integração com Commands

A camada de visão deve fornecer informações.

Por exemplo:

```text
Vision
  ↓
"Tag 5 está a 30 in, bearing -4°"
  ↓
Command
  ↓
decide o que fazer
```

Não:

```text
AprilTagCamera
  ↓
move drivetrain
```

Isso mantém a separação:

```text
Vision       → percepção
Localization → estado/pose
Commands     → decisão
Subsystems   → atuação
```

Essa separação é especialmente importante para a evolução futura do projeto.

---

# 21. Integração com Shooter

O `AprilTagCamera` não deve conhecer o Shooter.

Um fluxo futuro aceitável seria:

```text
AprilTagCamera
       │
       │ range
       ▼
CalcDistAlvo / camada de decisão
       │
       │ distância calculada
       ▼
Shooter Command
       │
       ▼
Shooter
```

Isso mantém a câmera independente do mecanismo que utiliza seus dados.

---

# 22. Achados classificados

### 🔴 Críticos

**Nenhum identificado no código apresentado.**

---

### 🟠 Importantes

**VISION-001 — API representa ausência de alvo como valores numéricos válidos**

Os getters retornam `0.0` quando não há detecção.

Isso pode causar confusão caso um consumidor esqueça de consultar `hasTarget()`.

**Ação:** considerar uma API de resultado explícito em uma futura evolução.

---

**VISION-002 — Política de seleção de alvo está embutida na câmera**

A câmera decide automaticamente:

```text
TARGET_APRIL_TAG_ID
        +
menor range
```

Isso é funcional, mas mistura percepção com uma política específica de seleção.

**Ação:** manter por enquanto; reavaliar quando houver necessidade de múltiplas políticas de alvo.

---

### 🟡 Melhorias

**VISION-003 — Nomenclatura inconsistente**

```java
DetectionProxima
RangeProximo
```

deveriam seguir a convenção Java:

```java
detectionProxima
rangeProximo
```

---

**VISION-004 — Configuração de visão está parcialmente misturada com configuração geral**

`WEBCAM` e `TARGET_APRIL_TAG_ID` estão em `RConstants`.

**Ação:** não alterar imediatamente; considerar uma futura `VisionConfig` quando a camada crescer.

---

**VISION-005 — `Vision` ainda não possui responsabilidade definida**

Não deve ser preenchida apenas para criar uma abstração artificial.

---

**VISION-006 — Limelight ainda não possui implementação**

Não constitui problema enquanto o hardware/uso não estiver definido.

---

### 🟢 Pontos positivos

**VISION-007 — Boa encapsulação da API FTC Vision**

Os consumidores não precisam acessar diretamente `AprilTagProcessor` ou `VisionPortal`.

---

**VISION-008 — Ciclo de vida explícito**

Existem operações claras de:

```text
init()
update()
close()
```

---

**VISION-009 — Detecção antiga não é mantida indevidamente**

Cada `update()` começa com:

```java
atualDetection = null;
```

evitando tratar uma detecção antiga como atual.

---

**VISION-010 — Filtragem de detecções inválidas**

São descartadas detecções sem:

```java
metadata
ftcPose
```

---

**VISION-011 — Seleção determinística**

Quando múltiplos alvos são válidos, a implementação seleciona o menor `range`.

---

# 23. Decisões recomendadas

## Fazer agora

1. Corrigir apenas a nomenclatura local:

   ```java
   DetectionProxima → detectionProxima
   RangeProximo → rangeProximo
   ```

2. Documentar a semântica de:

   ```java
   hasTarget()
   ```

3. Documentar que os getters retornam `0.0` quando não há alvo.

4. Manter `Vision` vazia até existir uma responsabilidade concreta.

5. Manter `Limelight` e `LimelightConfig` como placeholders até a definição do hardware e do uso.

---

## Não fazer agora

Não recomendo, neste estágio:

* criar uma abstração genérica `VisionTarget` apenas por antecipação;
* criar uma interface `VisionProvider`;
* integrar AprilTag diretamente com Localization;
* fazer `AprilTagCamera` controlar o Drive;
* mover lógica para Commands;
* implementar Limelight sem requisito concreto;
* adicionar filtros temporais sem evidência de instabilidade;
* alterar resolução sem testes de campo.

Essas mudanças aumentariam a complexidade sem evidência de que resolvem um problema atual.

---

# 24. Arquitetura-alvo sugerida

Uma evolução natural seria:

```text
                         Vision
                           │
             ┌─────────────┴─────────────┐
             │                           │
             ▼                           ▼
      AprilTagCamera                Limelight
             │                           │
             └─────────────┬─────────────┘
                           │
                           ▼
                    Vision observations
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
        Localization              Commands / Logic
              │                         │
              ▼                         ▼
        Robot Pose                 Robot actions
```

A ideia principal é que **Vision produza observações**, enquanto outras camadas decidam como utilizá-las.

---

# 25. Conclusão da AUD-006

A camada de Vision está em um estado **funcional, porém inicial**.

A implementação de `AprilTagCamera` já fornece uma abstração razoavelmente limpa sobre a API FTC Vision e apresenta um ciclo de vida coerente. Não foram identificados problemas críticos ou falhas estruturais que exijam uma reescrita.

O principal ponto arquitetural para as próximas auditorias é preservar a separação:

```text
Vision
  ↓
observação

Localization
  ↓
estimativa de pose

Commands
  ↓
decisão

Subsystems
  ↓
atuação
```

A existência de `Vision`, `Limelight` e `LimelightConfig` vazias deve ser entendida como **espaço reservado para evolução**, e não como algo que precise ser preenchido imediatamente.

### Estado final

| Item                                | Avaliação                          |
| ----------------------------------- | ---------------------------------- |
| `AprilTagCamera`                    | 🟢 Funcional                       |
| Ciclo de vida                       | 🟢 Adequado                        |
| Seleção de alvo                     | 🟢 Funcional                       |
| Encapsulamento FTC Vision           | 🟢 Bom                             |
| Tratamento de ausência de alvo      | 🟡 Melhorável                      |
| Configuração                        | 🟡 Pode evoluir                    |
| `Vision`                            | ⚪ Placeholder                      |
| `Limelight`                         | ⚪ Placeholder                      |
| Integração com Localization         | ⚪ Deve ser definida posteriormente |
| Necessidade de refatoração imediata | **Baixa**                          |
| Prioridade geral                    | **Baixa/Média**                    |

**AUD-006-V7sion: APROVADA COM MELHORIAS FUTURAS.**
