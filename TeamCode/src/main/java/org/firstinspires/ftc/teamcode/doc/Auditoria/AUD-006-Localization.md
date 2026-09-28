# AUD-005 — Localization

## 1. Objetivo da auditoria

Avaliar o estado atual da camada de **Localization**, identificando:

* responsabilidades atualmente implementadas;
* dependências entre localização, hardware e IMU;
* problemas de encapsulamento e integração;
* riscos de manutenção e evolução;
* lacunas para utilização efetiva da localização pelo restante do robô;
* responsabilidades que eventualmente deverão permanecer ou ser redistribuídas entre `Localization`, `Subsystems`, `OpModes` e demais camadas.

Esta auditoria considera o código atualmente fornecido e **não presume que a estrutura dos OpModes seja a arquitetura definitiva**, conforme estabelecido nas auditorias anteriores.

---

# 2. Escopo analisado

Foram fornecidas duas classes:

```text
org.firstinspires.ftc.teamcode.localization.GobildaOdometry
org.firstinspires.ftc.teamcode.localization.Localizer
```

A implementação efetiva encontra-se em `GobildaOdometry`.

`Localizer` está atualmente vazia.

---

# 3. Estado atual

## 3.1 `GobildaOdometry`

A classe realiza essencialmente uma configuração inicial do dispositivo GoBILDA Pinpoint.

O método:

```java
public void init(HardwareMap hardwareMap)
```

executa:

1. aquisição do dispositivo pelo `HardwareMap`;
2. configuração dos offsets;
3. configuração da resolução dos encoders;
4. configuração das direções dos encoders;
5. reset da posição e da IMU.

Trecho principal:

```java
odo = hardwareMap.get(GoBildaPinpointDriver.class, "odometry");

odo.setOffsets(-18, -10, DistanceUnit.CM);

odo.setEncoderResolution(
    GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD
);

odo.setEncoderDirections(
    GoBildaPinpointDriver.EncoderDirection.FORWARD,
    GoBildaPinpointDriver.EncoderDirection.FORWARD
);

odo.resetPosAndIMU();
```

Portanto, atualmente a classe funciona mais como um **configurador/inicializador do driver de odometria** do que como um sistema completo de localização.

---

# 4. `Localizer`

A classe:

```java
public class Localizer {
}
```

não possui implementação.

Consequentemente, no estado atual não existe uma abstração de localização capaz de:

* expor posição;
* expor orientação;
* atualizar a estimativa;
* fornecer pose ao restante do robô;
* combinar odometria com outras fontes;
* abstrair o fornecedor de hardware;
* definir uma API de localização para os demais componentes.

Isso é importante porque existe uma distinção arquitetural entre:

```text
GoBilda Pinpoint
       ↓
driver de hardware
       ↓
odometria
       ↓
localização
       ↓
pose do robô
```

e atualmente apenas uma parte dessa cadeia está implementada.

---

# 5. Achados

## LOC-001 — `GobildaOdometry` não fornece uma API de localização

**Severidade: Alta**

A classe configura o dispositivo, mas não disponibiliza métodos para consultar ou atualizar a localização.

Não existem métodos equivalentes a:

```java
getX()
getY()
getHeading()
getPose()
update()
```

ou uma API equivalente.

Assim, qualquer código que queira utilizar a localização precisa, potencialmente, acessar diretamente o `GoBildaPinpointDriver` ou depender de outra implementação ainda inexistente.

### Impacto

A camada `localization` não cumpre atualmente o papel de fornecer uma representação de localização ao restante do sistema.

### Recomendação

Definir, durante a evolução da arquitetura, uma API de localização independente do código de inicialização do hardware.

Não é necessário definir nesta auditoria qual API exata será utilizada; a decisão deve considerar como a localização será consumida por:

* autonomous;
* navegação;
* controle;
* vision;
* comandos;
* eventualmente outros subsistemas.

---

# 6. Achado LOC-002 — `Localizer` existe como intenção arquitetural, mas está vazia

**Severidade: Média**

A presença de:

```java
public class Localizer {
}
```

indica uma intenção de separar o conceito de **localizador** do driver específico da GoBILDA.

Entretanto, atualmente não existe implementação ou contrato associado.

Isso cria uma estrutura ambígua:

```text
localization/
├── GobildaOdometry
└── Localizer
```

mas não existe uma relação entre as duas classes.

### Interpretação

Não devemos concluir que `Localizer` necessariamente precisa ser uma classe concreta.

Ela pode eventualmente assumir diferentes papéis arquiteturais, por exemplo:

* interface;
* fachada;
* implementação de localização;
* camada de abstração;
* componente responsável pela pose.

A decisão deve ser tomada após analisar como a localização será utilizada pelo restante do projeto.

### Recomendação

Manter a distinção conceitual entre:

**hardware/driver de odometria**

e

**serviço/API de localização**,

mas evitar implementar abstrações artificiais apenas para preencher a estrutura de pacotes.

---

# 7. Achado LOC-003 — Dependência direta do hardware dentro da implementação

**Severidade: Média**

`GobildaOdometry` depende diretamente de:

```java
GoBildaPinpointDriver
```

Isso é esperado em uma camada de integração com hardware.

O problema não é a dependência em si, mas o fato de ela ser atualmente também o único mecanismo disponível para representar localização.

A arquitetura futura deveria evitar que o restante do sistema precise conhecer:

```java
GoBildaPinpointDriver
```

para obter a pose do robô.

### Recomendação

Manter o conhecimento específico do GoBILDA concentrado na camada de infraestrutura/localização.

As demais partes do software devem consumir uma representação de pose/localização definida pelo projeto.

---

# 8. Achado LOC-004 — Offsets são valores mágicos

**Severidade: Média**

Atualmente:

```java
odo.setOffsets(-18, -10, DistanceUnit.CM);
```

Os valores:

```text
-18 cm
-10 cm
```

estão diretamente no código.

Esses números representam uma característica física da montagem do sistema de odometria.

Portanto, não são simplesmente detalhes de implementação.

### Problema

Alterações mecânicas exigiriam alteração direta do código-fonte.

Além disso, não há indicação no código sobre:

* qual eixo corresponde a cada valor;
* ponto de referência utilizado;
* como esses valores foram medidos;
* qual configuração mecânica eles representam.

### Recomendação

Mover esses valores para uma configuração centralizada, provavelmente junto às constantes físicas do robô, caso isso seja compatível com a organização estabelecida em `RConstants`.

Entretanto, deve-se preservar a distinção entre:

**constantes físicas/configuração do robô**

e

**configuração específica de implementação do driver**.

---

# 9. Achado LOC-005 — Configuração de encoder também está embutida na implementação

**Severidade: Baixa/Média**

O código utiliza:

```java
GoBildaOdometryPods.goBILDA_4_BAR_POD
```

Isso é uma informação relacionada ao hardware instalado.

Diferentemente dos offsets, aqui a dependência com a API do fabricante é bastante natural.

Portanto, não há necessidade imediata de transformar isso em uma abstração complexa.

A principal recomendação é documentar que essa configuração corresponde ao hardware efetivamente instalado no robô.

---

# 10. Achado LOC-006 — Direções dos encoders não estão documentadas

**Severidade: Média**

Atualmente:

```java
odo.setEncoderDirections(
    GoBildaPinpointDriver.EncoderDirection.FORWARD,
    GoBildaPinpointDriver.EncoderDirection.FORWARD
);
```

Ambos os encoders estão configurados como `FORWARD`.

O código não explica por quê.

Isso é particularmente relevante em odometria, porque uma configuração incorreta de direção pode produzir uma pose aparentemente válida em alguns movimentos e incorreta em outros.

### Recomendação

Documentar a configuração com base na montagem física e, idealmente, validar explicitamente:

* movimento longitudinal;
* movimento lateral;
* rotação.

A auditoria não deve assumir que `FORWARD/FORWARD` está correto apenas por estar atualmente no código.

---

# 11. Achado LOC-007 — `resetPosAndIMU()` acopla inicialização de pose e IMU

**Severidade: Alta para análise arquitetural**

A inicialização termina com:

```java
odo.resetPosAndIMU();
```

Isso merece atenção especial porque a localização do projeto utiliza um sistema que possui integração com IMU.

O reset simultâneo significa que a inicialização do componente atualmente estabelece também uma referência para a orientação.

Isso não é necessariamente errado.

Porém, a decisão arquitetural importante é:

> **quem é responsável pela referência de heading do robô?**

Esse ponto já apareceu como questão relevante na auditoria geral do projeto e deve permanecer aberto nesta auditoria.

### Questões que precisam ser resolvidas

É necessário determinar posteriormente:

1. A IMU do Pinpoint será a fonte principal de heading?
2. Existe outra IMU no robô?
3. A Vision poderá corrigir orientação?
4. O heading deve ser zerado no início de cada Autonomous?
5. O heading absoluto ou relativo será utilizado?
6. O sistema precisa suportar reset de pose sem reset físico/absoluto da orientação?
7. O restante do projeto precisa conhecer diretamente a origem do heading?

### Status

**Não resolvido nesta auditoria.**

Esse ponto deve ser tratado como decisão arquitetural posterior, e não como bug confirmado.

---

# 12. Achado LOC-008 — `initTime` não possui utilização

A classe declara:

```java
double initTime = 0;
```

mas não utiliza essa variável.

**Severidade: Baixa**

Atualmente ela não possui efeito funcional.

### Recomendação

Remover caso não exista uma necessidade identificada.

Se houver intenção futura de:

* medir tempo desde inicialização;
* controlar atualização;
* calcular delta time;

isso deve ser implementado explicitamente quando necessário.

Não há justificativa atual para manter estado morto.

---

# 13. Achado LOC-009 — Estado interno possui encapsulamento insuficiente

O campo:

```java
GoBildaPinpointDriver odo;
```

não é declarado como `private`.

Além disso:

```java
double initTime = 0;
```

também não possui modificador de acesso.

### Recomendação

Caso esses campos permaneçam, utilizar encapsulamento explícito:

```java
private GoBildaPinpointDriver odo;
```

e remover `initTime` enquanto não possuir função.

---

# 14. Achado LOC-010 — Não existe ciclo de atualização

**Severidade: Alta**

A implementação possui apenas:

```java
init(...)
```

Não existe um método de atualização periódica.

Em um sistema de localização, normalmente existe uma sequência conceitual semelhante a:

```text
inicialização
     ↓
leitura dos sensores
     ↓
atualização da pose
     ↓
consumo da pose
```

Atualmente temos somente:

```text
inicialização
```

### Impacto

Ainda não existe um mecanismo definido para que a localização seja atualizada durante a execução do Autonomous.

### Recomendação

Definir posteriormente o ciclo de vida da localização.

Por exemplo:

```text
init()
update()
getPose()
```

A forma exata depende da arquitetura que será escolhida para os OpModes e Commands.

---

# 15. Achado LOC-011 — Não há representação explícita de Pose

**Severidade: Alta**

Não existe atualmente uma estrutura representando:

```text
X
Y
Heading
```

ou equivalente.

Isso dificulta estabelecer uma fronteira clara entre:

* sensores;
* odometria;
* localização;
* navegação.

### Recomendação

Definir uma representação de pose que possa ser utilizada pelo sistema sem expor diretamente o driver GoBILDA.

Essa representação deve deixar explícitas as unidades utilizadas.

---

# 16. Achado LOC-012 — Unidades precisam ser padronizadas

O código utiliza:

```java
DistanceUnit.CM
```

para os offsets.

Isso é positivo porque a unidade está explicitamente declarada.

Entretanto, ainda não existe uma política de unidades para a API de localização, porque não existe API de localização.

### Recomendação

Quando a API for criada, definir explicitamente:

* unidade de X;
* unidade de Y;
* unidade angular;
* convenção de heading;
* origem da coordenada;
* sentido positivo dos eixos.

Esse contrato é particularmente importante para integração com Vision e navegação.

---

# 17. Arquitetura atual identificada

O estado atual pode ser representado assim:

```text
                 HardwareMap
                     │
                     ▼
          GoBildaPinpointDriver
                     │
                     ▼
              GobildaOdometry
                     │
             configuração
                     │
                     ▼
             resetPosAndIMU()


              Localizer
                  │
                  │
              [vazio]
```

Não existe atualmente uma cadeia funcional completa:

```text
Hardware
   ↓
Odometry
   ↓
Localization
   ↓
Pose
   ↓
Navigation / Commands / Autonomous
```

---

# 18. Relação com Vision

Um ponto importante para as próximas etapas é que a localização não deve ser analisada isoladamente da Vision.

O projeto já possui uma camada de AprilTag/Vision, e portanto existe potencialmente mais de uma fonte de informação espacial:

```text
Odometry
    │
    ├── X
    ├── Y
    └── Heading
       
Vision / AprilTag
    │
    └── referência espacial
```

A auditoria atual não deve determinar antecipadamente se essas fontes precisam ser fundidas.

Entretanto, a arquitetura deve permitir que essa questão seja tratada posteriormente sem que os OpModes precisem conhecer detalhes do `GoBildaPinpointDriver`.

---

# 19. Relação com IMU

A utilização:

```java
resetPosAndIMU();
```

torna a IMU parte relevante do comportamento da inicialização.

Isso cria uma questão que deverá ser consolidada junto com a análise da IMU:

### Responsabilidades possíveis

```text
IMU
 └── orientação instantânea

Odometry
 └── deslocamento relativo

Localization
 └── pose consolidada
```

ou, dependendo do funcionamento do Pinpoint:

```text
Pinpoint
 ├── odometria
 └── heading baseado em IMU
       ↓
Localization
```

Não devemos escolher entre essas alternativas apenas com os arquivos atuais.

---

# 20. O que a camada deveria eventualmente fornecer

Sem impor ainda uma implementação concreta, a auditoria identifica como necessidade arquitetural uma interface conceitual semelhante a:

```text
Localization
│
├── inicializar
├── atualizar
├── obter posição X
├── obter posição Y
├── obter heading
└── obter pose
```

Possivelmente também:

```text
├── reset pose
└── set pose inicial
```

Mas esses últimos devem ser avaliados em conjunto com o comportamento desejado do Autonomous.

---

# 21. Responsabilidades que não deveriam ser misturadas

A auditoria recomenda preservar, conceitualmente, a separação:

### `GobildaOdometry`

Responsável por detalhes do hardware GoBILDA:

```text
HardwareMap
configuração dos pods
offsets
direções
resolução
acesso ao driver
```

### `Localizer`

Responsável pelo conceito de localização:

```text
pose
atualização
contrato de acesso
integração das fontes de localização
```

### OpMode

Deve consumir localização para executar comportamento do Autonomous, mas não deveria precisar reproduzir:

```java
setOffsets(...)
setEncoderResolution(...)
setEncoderDirections(...)
```

---

# 22. O que não foi considerado problema

Alguns aspectos são aceitáveis no estado atual:

### Uso direto do `HardwareMap`

É apropriado para uma camada responsável pela inicialização do hardware.

### Uso de `GoBildaPinpointDriver`

É esperado que a implementação específica do GoBILDA conheça a API do fabricante.

### Configuração no método `init`

A configuração inicial do dispositivo pertence naturalmente ao ciclo de inicialização.

### `DistanceUnit.CM`

O uso explícito de unidade é positivo e deve ser preservado.

---

# 23. Resumo dos achados

| ID      | Achado                           |  Severidade | Estado               |
| ------- | -------------------------------- | ----------: | -------------------- |
| LOC-001 | Não existe API de localização    |        Alta | Aberto               |
| LOC-002 | `Localizer` está vazia           |       Média | Aberto               |
| LOC-003 | Dependência direta do driver     |       Média | Arquitetural         |
| LOC-004 | Offsets são valores mágicos      |       Média | Aberto               |
| LOC-005 | Configuração do pod embutida     | Baixa/Média | Aceitável            |
| LOC-006 | Direções não documentadas        |       Média | Aberto               |
| LOC-007 | Reset de posição e IMU acoplados |        Alta | Decisão arquitetural |
| LOC-008 | `initTime` não utilizado         |       Baixa | Correção simples     |
| LOC-009 | Encapsulamento insuficiente      |       Baixa | Correção simples     |
| LOC-010 | Não existe ciclo de atualização  |        Alta | Aberto               |
| LOC-011 | Não existe representação de Pose |        Alta | Aberto               |
| LOC-012 | Contrato de unidades inexistente |       Média | Aberto               |

---

# 24. Prioridades

## Prioridade 1 — Definir o contrato de localização

Antes de implementar funcionalidades adicionais, definir o que o restante do robô entende por:

```text
Pose
X
Y
Heading
```

e quais unidades/convenções serão utilizadas.

---

## Prioridade 2 — Definir ciclo de vida

Determinar onde e quando ocorrerão:

```text
init
update
reset
getPose
```

Isso precisa ser compatível com a arquitetura dos OpModes/Commands.

---

## Prioridade 3 — Resolver a responsabilidade da IMU

Definir formalmente:

* quem fornece heading;
* quando o heading é zerado;
* como ocorre o reset;
* se Vision pode corrigir orientação;
* se haverá mais de uma fonte de orientação.

Esse é um ponto arquitetural e não deve ser resolvido apenas adicionando código à `GobildaOdometry`.

---

## Prioridade 4 — Isolar o hardware da API consumida pelo restante do projeto

O restante do software deve trabalhar com uma abstração de localização/pose, e não depender diretamente do:

```java
GoBildaPinpointDriver
```

---

## Prioridade 5 — Centralizar configuração física

Avaliar a migração dos:

```java
-18 cm
-10 cm
```

para a configuração física central do robô, mantendo documentada a relação desses valores com a montagem.

---

# 25. Conclusão da AUD-005

A camada de Localization está **em estágio inicial de implementação**.

O código existente demonstra que o projeto já possui uma integração concreta com o **GoBILDA Pinpoint**, incluindo configuração dos pods, offsets, direções e reset da posição/IMU.

Porém, ainda não existe efetivamente uma camada de localização consumível pelo restante do sistema.

O principal ponto identificado não é um erro isolado no `GobildaOdometry`, mas uma **lacuna arquitetural entre o hardware de odometria e o conceito de localização do robô**.

O próximo passo não deve ser simplesmente adicionar métodos à classe atual. Primeiro deve ser definido o contrato de localização do projeto — especialmente **Pose, unidades, heading, ciclo de atualização e relação com a IMU**. A partir disso será possível decidir corretamente o papel de `GobildaOdometry` e de `Localizer`.

### Estado da auditoria

**AUD-005 — Localization: CONCLUÍDA**

### Situação geral

```text
Integração GoBILDA Pinpoint       ✓ Existe
Configuração dos pods             ✓ Existe
Reset de posição/IMU              ✓ Existe
API de localização                ✗ Ausente
Representação de Pose              ✗ Ausente
Ciclo de atualização               ✗ Ausente
Contrato de heading                ✗ Não definido
Integração Odometry → Localizer    ✗ Ausente
Integração Localization → sistema  ✗ Ausente
```

**Recomendação central:** tratar a `GobildaOdometry` atual como a implementação de infraestrutura/hardware existente e, antes de expandi-la, estabelecer o contrato arquitetural de `Localization` e `Pose`.
