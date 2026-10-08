# AUD-009 — Utils

## Escopo

Esta auditoria avalia as classes utilitárias apresentadas:

* `CalcDistAlvo`
* `MathU`

O foco é verificar **responsabilidade, corretude, robustez, acoplamento, clareza da API e aderência à arquitetura em evolução do projeto FTC 31211**, considerando também as conclusões das auditorias anteriores.

---

## 1. Resumo executivo

As classes apresentadas são pequenas e possuem responsabilidades relativamente bem delimitadas. Não há, no trecho analisado, um problema arquitetural grave que exija reescrita imediata.

O principal ponto de atenção está em `CalcDistAlvo`: apesar de estar em `util`, ela contém **lógica específica do robô e do sistema de tiro/visão**, incluindo calibração de câmera, offsets mecânicos, filtragem e cálculo de distância para o shooter. Portanto, sua classificação como simples utilitário é discutível.

`MathU`, por outro lado, está bem alinhada com a ideia de uma classe utilitária matemática, embora possa receber pequenas melhorias de nomenclatura, documentação e validação.

### Classificação geral

| Área                                | Avaliação             |
| ----------------------------------- | --------------------- |
| Corretude aparente                  | 🟢 Boa                |
| Responsabilidade de `MathU`         | 🟢 Adequada           |
| Responsabilidade de `CalcDistAlvo`  | 🟡 Atenção            |
| Acoplamento com `RConstants`        | 🟡 Moderado           |
| Estado interno                      | 🟡 Atenção            |
| Tratamento de valores inválidos     | 🟡 Parcial            |
| Testabilidade                       | 🟢 Boa, com ressalvas |
| Necessidade de refatoração imediata | 🔴 Não                |
| Necessidade de revisão arquitetural | 🟡 Sim                |

**Conclusão:** manter as funcionalidades atuais, mas tratar `CalcDistAlvo` como candidata a futura reorganização arquitetural, especialmente quando a camada de Vision/Localization/Commands for consolidada.

---

# 2. `MathU`

## 2.1 Responsabilidade

A classe reúne três operações matemáticas genéricas:

```java
clamp(...)
aplicarZonaNeutra(...)
normalizarAngulo(...)
```

Isso está coerente com uma classe utilitária.

Não há dependência de hardware, subsistemas ou estado do robô.

### Avaliação

**🟢 Aprovado**

A classe pode permanecer em:

```text
util/MathU.java
```

---

# 3. `MathU.clamp()`

```java
public static double clamp(double value, double min, double max) {
    if (min > max) {
        double temp = min;
        min = max;
        max = temp;
    }

    return Math.max(min, Math.min(max, value));
}
```

## Pontos positivos

A implementação é simples e correta.

Também existe uma decisão interessante:

```java
if (min > max) {
    ...
}
```

Ou seja, a função não quebra quando recebe os limites invertidos.

Isso é particularmente útil em código de controle, onde uma chamada incorreta não necessariamente deve causar uma falha catastrófica.

### Avaliação

**🟢 Sem problema funcional identificado.**

---

# 4. `MathU.aplicarZonaNeutra()`

```java
public static double aplicarZonaNeutra(double value, double deadband) {
    deadband = Math.abs(deadband);

    if (Math.abs(value) < deadband) {
        return 0.0;
    }

    return value;
}
```

A função implementa uma deadband.

Exemplo:

```text
deadband = 0.1

 0.05 → 0
-0.05 → 0
 0.10 → 0.10
-0.10 → -0.10
```

Isso é coerente com uma zona neutra convencional.

### Ponto de atenção

Existe uma decisão implícita sobre o limite:

```java
Math.abs(value) < deadband
```

Portanto, exatamente no limite a entrada **não é anulada**.

Isso não é necessariamente errado. É uma escolha válida, mas deveria ser conhecida pelo projeto.

### Avaliação

**🟢 Correto.**

**🟡 Documentação recomendada:** explicitar que o limite da deadband é inclusivo para o valor retornado.

---

# 5. `MathU.normalizarAngulo()`

```java
public static double normalizarAngulo(double angle) {
    angle %= 360.0;

    if (angle > 180.0) {
        angle -= 360.0;
    }

    if (angle < -180.0) {
        angle += 360.0;
    }

    return angle;
}
```

A intenção é produzir:

```text
[-180°, +180°]
```

Exemplos:

```text
 270° → -90°
 360° →   0°
 450° →  90°
-270° →  90°
```

A implementação atende a essa intenção.

### Ponto de atenção

Os valores:

```text
180°
-180°
```

são ambos possíveis.

Isso é aceitável, mas significa que a função não produz uma representação única para a direção exatamente oposta.

Para controle de heading isso normalmente não é um problema, desde que o restante do sistema trate ambos de maneira consistente.

### Avaliação

**🟢 Adequado.**

---

# 6. `CalcDistAlvo`

Aqui está o principal ponto da auditoria.

A classe está em:

```text
org.firstinspires.ftc.teamcode.util
```

mas seu conteúdo é altamente específico do robô.

Ela conhece:

* AprilTag;
* câmera;
* shooter;
* distância câmera → shooter;
* distância tag → alvo;
* calibração da câmera;
* filtro usado pelo autoshooter;
* distância inicial do autônomo;
* deslocamento obtido pelos encoders.

Isso ultrapassa bastante o conceito tradicional de uma classe utilitária genérica.

---

# 7. `getCameraDistanceCm()`

```java
public double getCameraDistanceCm(double aprilTagRangeInches) {
    double safeRangeInches = Math.max(0.0, aprilTagRangeInches);

    return safeRangeInches * RConstants.INCH_TO_CM;
}
```

A conversão:

```text
inch → cm
```

está correta conceitualmente.

Também existe proteção contra distância negativa.

### Ponto de atenção

A função trata qualquer valor negativo como:

```text
0 cm
```

Isso é conveniente, mas também pode esconder erro de entrada.

Por exemplo, uma leitura inválida poderia ser transformada silenciosamente em uma distância válida de `0`.

Para o código atual isso não constitui bug comprovado, mas é uma decisão semântica importante.

### Avaliação

**🟢 Funcionalmente adequada.**

**🟡 Registrar que valores inválidos são saturados para zero.**

---

# 8. `getCalibratedCameraDistanceCm()`

```java
return Math.max(
        0.0,
        rawCameraDistanceCm
                * RConstants.CAMERA_DISTANCE_SCALE
                + RConstants.CAMERA_DISTANCE_BIAS_CM
);
```

A fórmula é essencialmente:

```text
distância_calibrada =
    distância_bruta × escala + offset
```

Isso é uma calibração linear válida.

O uso de constantes externas também evita espalhar números mágicos pelo código.

### Ponto arquitetural

Entretanto:

```java
RConstants.CAMERA_DISTANCE_SCALE
RConstants.CAMERA_DISTANCE_BIAS_CM
```

fazem parte da configuração específica do robô.

Isso reforça que `CalcDistAlvo` não é realmente uma classe matemática genérica.

---

# 9. `getShooterDistanceCm()`

```java
double shooterDistanceCm =
        calibratedCameraDistanceCm
                + RConstants.CAMERA_TO_SHOOTER_OFFSET_CM
                + RConstants.TAG_TO_TARGET_OFFSET_CM;
```

A sequência conceitual é:

```text
AprilTag
   ↓
distância da câmera
   ↓
calibração
   ↓
offset câmera → shooter
   ↓
offset tag → alvo
   ↓
distância usada pelo shooter
```

Essa cadeia faz sentido.

### Ponto importante

Essa função representa **conhecimento físico do robô**, e não uma simples operação matemática.

Portanto, é um dos principais argumentos para futuramente retirar essa lógica de `util`.

---

# 10. Filtro de distância

```java
private boolean hasFilteredDistance = false;
private double filteredShooterDistanceCm = 0.0;
```

e:

```java
filteredShooterDistanceCm =
        alpha * measuredDistanceCm
                + (1.0 - alpha) * filteredShooterDistanceCm;
```

Isso é um filtro exponencial simples, equivalente a um:

```text
EMA — Exponential Moving Average
```

A implementação está correta.

A primeira leitura também é tratada especialmente:

```java
if (!hasFilteredDistance) {
    filteredShooterDistanceCm = measuredDistanceCm;
    hasFilteredDistance = true;
    return filteredShooterDistanceCm;
}
```

Isso evita começar o filtro em:

```text
0 cm
```

e criar artificialmente uma sequência inicial de valores baixos.

### Avaliação

**🟢 Boa implementação.**

---

# 11. `resetDistanceFilter()`

```java
public void resetDistanceFilter() {
    hasFilteredDistance = false;
    filteredShooterDistanceCm = 0.0;
}
```

A função é necessária porque o filtro possui estado.

O comentário também explica corretamente o uso:

```java
/** Reinicia o filtro após perder a AprilTag ou trocar de alvo. */
```

### Ponto arquitetural

Esse estado interno transforma `CalcDistAlvo` em um objeto com comportamento temporal.

Isso é importante porque classes puramente utilitárias normalmente seriam:

```java
static
```

e sem estado.

Aqui temos:

```text
entrada
  ↓
estado interno
  ↓
saída
```

Portanto, `CalcDistAlvo` está mais próximo de um **componente de processamento de distância** do que de um utilitário matemático.

---

# 12. Validação do `alpha`

```java
double alpha = MathU.clamp(
        RConstants.CAMERA_DISTANCE_FILTER_ALPHA,
        0.0,
        1.0
);
```

Boa decisão.

Mesmo que a constante esteja configurada incorretamente, o filtro continua matematicamente limitado a:

```text
0 ≤ alpha ≤ 1
```

### Avaliação

**🟢 Boa proteção.**

---

# 13. Distância por encoder

```java
public double getShooterDistanceFromEncoderCm(
        double startDistanceToGoalCm,
        double traveledTowardGoalCm
) {
    double safeStartDistance = Math.max(0.0, startDistanceToGoalCm);
    double safeTraveledDistance = Math.max(0.0, traveledTowardGoalCm);

    return Math.max(0.0, safeStartDistance - safeTraveledDistance);
}
```

A fórmula:

```text
distância atual =
    distância inicial - deslocamento
```

é coerente para o caso explicitamente documentado:

> robô parte de uma posição conhecida e se desloca em linha reta na direção do gol.

### Limitação importante

A função pressupõe que:

```text
traveledTowardGoalCm
```

já representa **distância efetivamente percorrida na direção do alvo**.

Ela não calcula isso a partir de encoder.

Portanto, não existe aqui nenhuma transformação:

```text
ticks → cm
```

nem consideração de:

* orientação do robô;
* trajetória;
* erro de odometria;
* movimento lateral;
* rotação;
* distância geométrica até o alvo.

Isso não é um defeito da função, desde que sua responsabilidade permaneça exatamente a descrita no comentário.

---

# 14. Problema arquitetural principal

A maior questão da auditoria não é um bug matemático.

É a localização da responsabilidade.

Atualmente:

```text
util/
├── CalcDistAlvo.java
└── MathU.java
```

mas conceitualmente temos:

```text
MathU
└── matemática genérica

CalcDistAlvo
├── Vision
├── calibração
├── geometria do robô
├── Shooter
├── filtragem
└── Autônomo / Encoder
```

São responsabilidades de naturezas diferentes.

Isso cria uma classe que pode acabar se tornando um "depósito" de cálculos relacionados ao robô.

Esse risco deve ser evitado nas próximas auditorias.

---

# 15. Relação com as auditorias anteriores

Isso se conecta diretamente com o que já apareceu nas auditorias de:

* Vision;
* Localization;
* Shooter;
* Control & Commands.

Existe uma separação natural que começa a aparecer:

```text
Vision
    ↓
medição

Localization
    ↓
posição / deslocamento

Control
    ↓
controle

Shooter
    ↓
uso da distância

Commands
    ↓
orquestração
```

`CalcDistAlvo` atualmente atravessa várias dessas fronteiras.

Por exemplo:

```text
Vision → CalcDistAlvo → Shooter
```

e também:

```text
Localization/Encoder → CalcDistAlvo
```

Isso deve ser observado antes de qualquer refatoração.

---

# 16. Recomendação arquitetural

**Não recomendo mover a classe imediatamente.**

O código atual deve continuar funcionando enquanto a arquitetura geral ainda está sendo consolidada.

A recomendação é registrar uma futura decomposição conceitual.

Uma possível arquitetura futura seria:

```text
vision/
    AprilTag...
    
localization/
    ...

shooter/
    ShooterDistanceCalculator
    DistanceFilter

util/
    MathU
```

Ou, dependendo da arquitetura final:

```text
util/
    MathU

subsystems/shooter/
    ShooterDistanceModel

vision/
    VisionDistance...
```

A escolha exata deve ser feita somente depois de concluídas as auditorias de Vision, Localization, Shooter e Commands.

---

# 17. Testes recomendados

Estas classes são excelentes candidatas a testes unitários.

### `MathU`

Testar:

```text
clamp(5, 0, 10)       → 5
clamp(-1, 0, 10)      → 0
clamp(20, 0, 10)      → 10
clamp(5, 10, 0)       → 5

deadband(0.05, 0.10)  → 0
deadband(-0.05, 0.10) → 0
deadband(0.10, 0.10)  → 0.10

normalize(0)          → 0
normalize(180)        → 180
normalize(270)        → -90
normalize(-270)       → 90
normalize(360)        → 0
```

### `CalcDistAlvo`

Devem existir testes para:

```text
inch → cm
calibração
offsets
valores negativos
primeira leitura do filtro
segunda leitura do filtro
alpha = 0
alpha = 1
reset do filtro
distância por encoder
```

Especialmente:

```text
resetDistanceFilter()
```

deve garantir que a próxima leitura seja novamente tratada como primeira leitura.

---

# 18. Pontos que não devem ser alterados sem evidência

Não há evidência suficiente nesta auditoria para alterar:

```java
CAMERA_DISTANCE_SCALE
CAMERA_DISTANCE_BIAS_CM
CAMERA_TO_SHOOTER_OFFSET_CM
TAG_TO_TARGET_OFFSET_CM
CAMERA_DISTANCE_FILTER_ALPHA
```

Esses valores pertencem à calibração/configuração física do robô.

A auditoria deve verificar **como eles são utilizados**, não substituir seus valores sem dados de campo.

Da mesma forma, não há justificativa para substituir o filtro EMA por outro filtro apenas por preferência arquitetural.

---

# 19. Achados

| ID      | Severidade | Achado                                                               | Ação                                                         |
| ------- | ---------- | -------------------------------------------------------------------- | ------------------------------------------------------------ |
| UTL-001 | 🟡 Média   | `CalcDistAlvo` possui responsabilidade específica demais para `util` | Registrar para futura refatoração                            |
| UTL-002 | 🟡 Média   | `CalcDistAlvo` mistura Vision, geometria do robô, Shooter e Encoder  | Separar responsabilidades quando arquitetura for consolidada |
| UTL-003 | 🟢 Baixa   | Valores negativos são silenciosamente convertidos para zero          | Manter por enquanto; documentar comportamento                |
| UTL-004 | 🟢 Baixa   | `MathU` possui deadband com limite inclusivo                         | Manter; documentar semântica                                 |
| UTL-005 | 🟢 Baixa   | `normalizarAngulo()` permite `-180` e `180`                          | Aceitável; garantir consistência nos consumidores            |
| UTL-006 | 🟢 Baixa   | Falta de testes unitários explícitos no material apresentado         | Adicionar posteriormente                                     |
| UTL-007 | 🟢 Baixa   | `CalcDistAlvo` mantém estado interno                                 | Não é erro; reforça que não é utilitário puramente estático  |

---

# 20. Decisão da auditoria

### `MathU`

**🟢 APROVADO**

Pode permanecer como utilitário matemático.

Não há necessidade de refatoração estrutural.

---

### `CalcDistAlvo`

**🟡 APROVADO COM RESSALVA ARQUITETURAL**

O comportamento apresentado é coerente e não há bug evidente que justifique uma alteração imediata.

Entretanto, a classe **não deve crescer indefinidamente** dentro de `util`.

A futura arquitetura deve decidir onde ficam:

* conversão/calibração de distância da Vision;
* modelo geométrico entre câmera, AprilTag, alvo e shooter;
* filtro de distância;
* cálculo baseado em encoder.

---

# 21. Prioridade

**Prioridade imediata: baixa.**

Não recomendo interromper a evolução do robô para refatorar essas classes agora.

A ordem recomendada é:

```text
AUD-008 Utils
    ↓
registrar achados
    ↓
concluir arquitetura das camadas
    ↓
revisar Vision
    ↓
revisar Localization
    ↓
revisar Shooter
    ↓
revisar Commands/Control
    ↓
refatorar responsabilidades cruzadas
    ↓
testes unitários
```

O ponto mais importante desta auditoria é, portanto, **não tratar `CalcDistAlvo` como um simples utilitário só porque está dentro de `util/`**. A localização física do arquivo e a responsabilidade arquitetural da classe ainda não estão perfeitamente alinhadas.

**Status final: 🟢 MathU / 🟡 CalcDistAlvo — sem correção obrigatória imediata.**
# AUD-009 — Utils

## Escopo

Esta auditoria avalia as classes utilitárias apresentadas:

* `CalcDistAlvo`
* `MathU`

O foco é verificar **responsabilidade, corretude, robustez, acoplamento, clareza da API e aderência à arquitetura em evolução do projeto FTC 31211**, considerando também as conclusões das auditorias anteriores.

---

## 1. Resumo executivo

As classes apresentadas são pequenas e possuem responsabilidades relativamente bem delimitadas. Não há, no trecho analisado, um problema arquitetural grave que exija reescrita imediata.

O principal ponto de atenção está em `CalcDistAlvo`: apesar de estar em `util`, ela contém **lógica específica do robô e do sistema de tiro/visão**, incluindo calibração de câmera, offsets mecânicos, filtragem e cálculo de distância para o shooter. Portanto, sua classificação como simples utilitário é discutível.

`MathU`, por outro lado, está bem alinhada com a ideia de uma classe utilitária matemática, embora possa receber pequenas melhorias de nomenclatura, documentação e validação.

### Classificação geral

| Área                                | Avaliação             |
| ----------------------------------- | --------------------- |
| Corretude aparente                  | 🟢 Boa                |
| Responsabilidade de `MathU`         | 🟢 Adequada           |
| Responsabilidade de `CalcDistAlvo`  | 🟡 Atenção            |
| Acoplamento com `RConstants`        | 🟡 Moderado           |
| Estado interno                      | 🟡 Atenção            |
| Tratamento de valores inválidos     | 🟡 Parcial            |
| Testabilidade                       | 🟢 Boa, com ressalvas |
| Necessidade de refatoração imediata | 🔴 Não                |
| Necessidade de revisão arquitetural | 🟡 Sim                |

**Conclusão:** manter as funcionalidades atuais, mas tratar `CalcDistAlvo` como candidata a futura reorganização arquitetural, especialmente quando a camada de Vision/Localization/Commands for consolidada.

---

# 2. `MathU`

## 2.1 Responsabilidade

A classe reúne três operações matemáticas genéricas:

```java
clamp(...)
aplicarZonaNeutra(...)
normalizarAngulo(...)
```

Isso está coerente com uma classe utilitária.

Não há dependência de hardware, subsistemas ou estado do robô.

### Avaliação

**🟢 Aprovado**

A classe pode permanecer em:

```text
util/MathU.java
```

---

# 3. `MathU.clamp()`

```java
public static double clamp(double value, double min, double max) {
    if (min > max) {
        double temp = min;
        min = max;
        max = temp;
    }

    return Math.max(min, Math.min(max, value));
}
```

## Pontos positivos

A implementação é simples e correta.

Também existe uma decisão interessante:

```java
if (min > max) {
    ...
}
```

Ou seja, a função não quebra quando recebe os limites invertidos.

Isso é particularmente útil em código de controle, onde uma chamada incorreta não necessariamente deve causar uma falha catastrófica.

### Avaliação

**🟢 Sem problema funcional identificado.**

---

# 4. `MathU.aplicarZonaNeutra()`

```java
public static double aplicarZonaNeutra(double value, double deadband) {
    deadband = Math.abs(deadband);

    if (Math.abs(value) < deadband) {
        return 0.0;
    }

    return value;
}
```

A função implementa uma deadband.

Exemplo:

```text
deadband = 0.1

 0.05 → 0
-0.05 → 0
 0.10 → 0.10
-0.10 → -0.10
```

Isso é coerente com uma zona neutra convencional.

### Ponto de atenção

Existe uma decisão implícita sobre o limite:

```java
Math.abs(value) < deadband
```

Portanto, exatamente no limite a entrada **não é anulada**.

Isso não é necessariamente errado. É uma escolha válida, mas deveria ser conhecida pelo projeto.

### Avaliação

**🟢 Correto.**

**🟡 Documentação recomendada:** explicitar que o limite da deadband é inclusivo para o valor retornado.

---

# 5. `MathU.normalizarAngulo()`

```java
public static double normalizarAngulo(double angle) {
    angle %= 360.0;

    if (angle > 180.0) {
        angle -= 360.0;
    }

    if (angle < -180.0) {
        angle += 360.0;
    }

    return angle;
}
```

A intenção é produzir:

```text
[-180°, +180°]
```

Exemplos:

```text
 270° → -90°
 360° →   0°
 450° →  90°
-270° →  90°
```

A implementação atende a essa intenção.

### Ponto de atenção

Os valores:

```text
180°
-180°
```

são ambos possíveis.

Isso é aceitável, mas significa que a função não produz uma representação única para a direção exatamente oposta.

Para controle de heading isso normalmente não é um problema, desde que o restante do sistema trate ambos de maneira consistente.

### Avaliação

**🟢 Adequado.**

---

# 6. `CalcDistAlvo`

Aqui está o principal ponto da auditoria.

A classe está em:

```text
org.firstinspires.ftc.teamcode.util
```

mas seu conteúdo é altamente específico do robô.

Ela conhece:

* AprilTag;
* câmera;
* shooter;
* distância câmera → shooter;
* distância tag → alvo;
* calibração da câmera;
* filtro usado pelo autoshooter;
* distância inicial do autônomo;
* deslocamento obtido pelos encoders.

Isso ultrapassa bastante o conceito tradicional de uma classe utilitária genérica.

---

# 7. `getCameraDistanceCm()`

```java
public double getCameraDistanceCm(double aprilTagRangeInches) {
    double safeRangeInches = Math.max(0.0, aprilTagRangeInches);

    return safeRangeInches * RConstants.INCH_TO_CM;
}
```

A conversão:

```text
inch → cm
```

está correta conceitualmente.

Também existe proteção contra distância negativa.

### Ponto de atenção

A função trata qualquer valor negativo como:

```text
0 cm
```

Isso é conveniente, mas também pode esconder erro de entrada.

Por exemplo, uma leitura inválida poderia ser transformada silenciosamente em uma distância válida de `0`.

Para o código atual isso não constitui bug comprovado, mas é uma decisão semântica importante.

### Avaliação

**🟢 Funcionalmente adequada.**

**🟡 Registrar que valores inválidos são saturados para zero.**

---

# 8. `getCalibratedCameraDistanceCm()`

```java
return Math.max(
        0.0,
        rawCameraDistanceCm
                * RConstants.CAMERA_DISTANCE_SCALE
                + RConstants.CAMERA_DISTANCE_BIAS_CM
);
```

A fórmula é essencialmente:

```text
distância_calibrada =
    distância_bruta × escala + offset
```

Isso é uma calibração linear válida.

O uso de constantes externas também evita espalhar números mágicos pelo código.

### Ponto arquitetural

Entretanto:

```java
RConstants.CAMERA_DISTANCE_SCALE
RConstants.CAMERA_DISTANCE_BIAS_CM
```

fazem parte da configuração específica do robô.

Isso reforça que `CalcDistAlvo` não é realmente uma classe matemática genérica.

---

# 9. `getShooterDistanceCm()`

```java
double shooterDistanceCm =
        calibratedCameraDistanceCm
                + RConstants.CAMERA_TO_SHOOTER_OFFSET_CM
                + RConstants.TAG_TO_TARGET_OFFSET_CM;
```

A sequência conceitual é:

```text
AprilTag
   ↓
distância da câmera
   ↓
calibração
   ↓
offset câmera → shooter
   ↓
offset tag → alvo
   ↓
distância usada pelo shooter
```

Essa cadeia faz sentido.

### Ponto importante

Essa função representa **conhecimento físico do robô**, e não uma simples operação matemática.

Portanto, é um dos principais argumentos para futuramente retirar essa lógica de `util`.

---

# 10. Filtro de distância

```java
private boolean hasFilteredDistance = false;
private double filteredShooterDistanceCm = 0.0;
```

e:

```java
filteredShooterDistanceCm =
        alpha * measuredDistanceCm
                + (1.0 - alpha) * filteredShooterDistanceCm;
```

Isso é um filtro exponencial simples, equivalente a um:

```text
EMA — Exponential Moving Average
```

A implementação está correta.

A primeira leitura também é tratada especialmente:

```java
if (!hasFilteredDistance) {
    filteredShooterDistanceCm = measuredDistanceCm;
    hasFilteredDistance = true;
    return filteredShooterDistanceCm;
}
```

Isso evita começar o filtro em:

```text
0 cm
```

e criar artificialmente uma sequência inicial de valores baixos.

### Avaliação

**🟢 Boa implementação.**

---

# 11. `resetDistanceFilter()`

```java
public void resetDistanceFilter() {
    hasFilteredDistance = false;
    filteredShooterDistanceCm = 0.0;
}
```

A função é necessária porque o filtro possui estado.

O comentário também explica corretamente o uso:

```java
/** Reinicia o filtro após perder a AprilTag ou trocar de alvo. */
```

### Ponto arquitetural

Esse estado interno transforma `CalcDistAlvo` em um objeto com comportamento temporal.

Isso é importante porque classes puramente utilitárias normalmente seriam:

```java
static
```

e sem estado.

Aqui temos:

```text
entrada
  ↓
estado interno
  ↓
saída
```

Portanto, `CalcDistAlvo` está mais próximo de um **componente de processamento de distância** do que de um utilitário matemático.

---

# 12. Validação do `alpha`

```java
double alpha = MathU.clamp(
        RConstants.CAMERA_DISTANCE_FILTER_ALPHA,
        0.0,
        1.0
);
```

Boa decisão.

Mesmo que a constante esteja configurada incorretamente, o filtro continua matematicamente limitado a:

```text
0 ≤ alpha ≤ 1
```

### Avaliação

**🟢 Boa proteção.**

---

# 13. Distância por encoder

```java
public double getShooterDistanceFromEncoderCm(
        double startDistanceToGoalCm,
        double traveledTowardGoalCm
) {
    double safeStartDistance = Math.max(0.0, startDistanceToGoalCm);
    double safeTraveledDistance = Math.max(0.0, traveledTowardGoalCm);

    return Math.max(0.0, safeStartDistance - safeTraveledDistance);
}
```

A fórmula:

```text
distância atual =
    distância inicial - deslocamento
```

é coerente para o caso explicitamente documentado:

> robô parte de uma posição conhecida e se desloca em linha reta na direção do gol.

### Limitação importante

A função pressupõe que:

```text
traveledTowardGoalCm
```

já representa **distância efetivamente percorrida na direção do alvo**.

Ela não calcula isso a partir de encoder.

Portanto, não existe aqui nenhuma transformação:

```text
ticks → cm
```

nem consideração de:

* orientação do robô;
* trajetória;
* erro de odometria;
* movimento lateral;
* rotação;
* distância geométrica até o alvo.

Isso não é um defeito da função, desde que sua responsabilidade permaneça exatamente a descrita no comentário.

---

# 14. Problema arquitetural principal

A maior questão da auditoria não é um bug matemático.

É a localização da responsabilidade.

Atualmente:

```text
util/
├── CalcDistAlvo.java
└── MathU.java
```

mas conceitualmente temos:

```text
MathU
└── matemática genérica

CalcDistAlvo
├── Vision
├── calibração
├── geometria do robô
├── Shooter
├── filtragem
└── Autônomo / Encoder
```

São responsabilidades de naturezas diferentes.

Isso cria uma classe que pode acabar se tornando um "depósito" de cálculos relacionados ao robô.

Esse risco deve ser evitado nas próximas auditorias.

---

# 15. Relação com as auditorias anteriores

Isso se conecta diretamente com o que já apareceu nas auditorias de:

* Vision;
* Localization;
* Shooter;
* Control & Commands.

Existe uma separação natural que começa a aparecer:

```text
Vision
    ↓
medição

Localization
    ↓
posição / deslocamento

Control
    ↓
controle

Shooter
    ↓
uso da distância

Commands
    ↓
orquestração
```

`CalcDistAlvo` atualmente atravessa várias dessas fronteiras.

Por exemplo:

```text
Vision → CalcDistAlvo → Shooter
```

e também:

```text
Localization/Encoder → CalcDistAlvo
```

Isso deve ser observado antes de qualquer refatoração.

---

# 16. Recomendação arquitetural

**Não recomendo mover a classe imediatamente.**

O código atual deve continuar funcionando enquanto a arquitetura geral ainda está sendo consolidada.

A recomendação é registrar uma futura decomposição conceitual.

Uma possível arquitetura futura seria:

```text
vision/
    AprilTag...
    
localization/
    ...

shooter/
    ShooterDistanceCalculator
    DistanceFilter

util/
    MathU
```

Ou, dependendo da arquitetura final:

```text
util/
    MathU

subsystems/shooter/
    ShooterDistanceModel

vision/
    VisionDistance...
```

A escolha exata deve ser feita somente depois de concluídas as auditorias de Vision, Localization, Shooter e Commands.

---

# 17. Testes recomendados

Estas classes são excelentes candidatas a testes unitários.

### `MathU`

Testar:

```text
clamp(5, 0, 10)       → 5
clamp(-1, 0, 10)      → 0
clamp(20, 0, 10)      → 10
clamp(5, 10, 0)       → 5

deadband(0.05, 0.10)  → 0
deadband(-0.05, 0.10) → 0
deadband(0.10, 0.10)  → 0.10

normalize(0)          → 0
normalize(180)        → 180
normalize(270)        → -90
normalize(-270)       → 90
normalize(360)        → 0
```

### `CalcDistAlvo`

Devem existir testes para:

```text
inch → cm
calibração
offsets
valores negativos
primeira leitura do filtro
segunda leitura do filtro
alpha = 0
alpha = 1
reset do filtro
distância por encoder
```

Especialmente:

```text
resetDistanceFilter()
```

deve garantir que a próxima leitura seja novamente tratada como primeira leitura.

---

# 18. Pontos que não devem ser alterados sem evidência

Não há evidência suficiente nesta auditoria para alterar:

```java
CAMERA_DISTANCE_SCALE
CAMERA_DISTANCE_BIAS_CM
CAMERA_TO_SHOOTER_OFFSET_CM
TAG_TO_TARGET_OFFSET_CM
CAMERA_DISTANCE_FILTER_ALPHA
```

Esses valores pertencem à calibração/configuração física do robô.

A auditoria deve verificar **como eles são utilizados**, não substituir seus valores sem dados de campo.

Da mesma forma, não há justificativa para substituir o filtro EMA por outro filtro apenas por preferência arquitetural.

---

# 19. Achados

| ID      | Severidade | Achado                                                               | Ação                                                         |
| ------- | ---------- | -------------------------------------------------------------------- | ------------------------------------------------------------ |
| UTL-001 | 🟡 Média   | `CalcDistAlvo` possui responsabilidade específica demais para `util` | Registrar para futura refatoração                            |
| UTL-002 | 🟡 Média   | `CalcDistAlvo` mistura Vision, geometria do robô, Shooter e Encoder  | Separar responsabilidades quando arquitetura for consolidada |
| UTL-003 | 🟢 Baixa   | Valores negativos são silenciosamente convertidos para zero          | Manter por enquanto; documentar comportamento                |
| UTL-004 | 🟢 Baixa   | `MathU` possui deadband com limite inclusivo                         | Manter; documentar semântica                                 |
| UTL-005 | 🟢 Baixa   | `normalizarAngulo()` permite `-180` e `180`                          | Aceitável; garantir consistência nos consumidores            |
| UTL-006 | 🟢 Baixa   | Falta de testes unitários explícitos no material apresentado         | Adicionar posteriormente                                     |
| UTL-007 | 🟢 Baixa   | `CalcDistAlvo` mantém estado interno                                 | Não é erro; reforça que não é utilitário puramente estático  |

---

# 20. Decisão da auditoria

### `MathU`

**🟢 APROVADO**

Pode permanecer como utilitário matemático.

Não há necessidade de refatoração estrutural.

---

### `CalcDistAlvo`

**🟡 APROVADO COM RESSALVA ARQUITETURAL**

O comportamento apresentado é coerente e não há bug evidente que justifique uma alteração imediata.

Entretanto, a classe **não deve crescer indefinidamente** dentro de `util`.

A futura arquitetura deve decidir onde ficam:

* conversão/calibração de distância da Vision;
* modelo geométrico entre câmera, AprilTag, alvo e shooter;
* filtro de distância;
* cálculo baseado em encoder.

---

# 21. Prioridade

**Prioridade imediata: baixa.**

Não recomendo interromper a evolução do robô para refatorar essas classes agora.

A ordem recomendada é:

```text
AUD-008 Utils
    ↓
registrar achados
    ↓
concluir arquitetura das camadas
    ↓
revisar Vision
    ↓
revisar Localization
    ↓
revisar Shooter
    ↓
revisar Commands/Control
    ↓
refatorar responsabilidades cruzadas
    ↓
testes unitários
```

O ponto mais importante desta auditoria é, portanto, **não tratar `CalcDistAlvo` como um simples utilitário só porque está dentro de `util/`**. A localização física do arquivo e a responsabilidade arquitetural da classe ainda não estão perfeitamente alinhadas.

**Status final: 🟢 MathU / 🟡 CalcDistAlvo — sem correção obrigatória imediata.**
