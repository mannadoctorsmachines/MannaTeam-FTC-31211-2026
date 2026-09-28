# AUD-005 — Viper

## 1. Identificação

* **ID:** AUD-005-Viper
* **Área:** `subsystems.viper`
* **Arquivos analisados:**

    * `Viper.java`
    * `ViperConfig.java`
* **Estado atual:** Estrutura inicial / não implementada
* **Objetivo da auditoria:** Avaliar a estrutura atual do subsistema Viper, identificar responsabilidades já definidas, lacunas de implementação e estabelecer uma direção arquitetural coerente com a organização do projeto.

---

## 2. Código analisado

### `Viper.java`

```java
package org.firstinspires.ftc.teamcode.subsystems.viper;

public class Viper {
}
```

### `ViperConfig.java`

```java
package org.firstinspires.ftc.teamcode.subsystems.viper;

public class ViperConfig {
}
```

---

# 3. Diagnóstico

## 3.1 Estado de implementação

O subsistema Viper encontra-se atualmente apenas como uma estrutura de classes vazias.

`Viper` não possui:

* referências a hardware;
* construtor;
* inicialização via `HardwareMap`;
* métodos de controle;
* métodos de leitura de estado;
* controle de motores;
* controle de servos;
* limites de operação;
* tratamento de estados;
* integração com comandos;
* integração com outras partes do robô.

`ViperConfig` também não possui nenhum campo, constante ou comportamento.

Portanto, **não existe atualmente uma implementação funcional do subsistema Viper**.

---

# 4. Responsabilidade do subsistema

A partir dos arquivos fornecidos, **não é possível determinar com segurança qual é a função mecânica exata do Viper**.

O nome `Viper` identifica o subsistema, mas não é suficiente para determinar se ele representa, por exemplo:

* um mecanismo linear;
* um conjunto de motores;
* um elevador;
* um sistema de extensão/retração;
* um mecanismo específico da configuração mecânica atual do robô.

Essa definição não deve ser inferida apenas pelo nome da classe.

### Decisão da auditoria

A responsabilidade mecânica do Viper deve ser documentada antes da implementação definitiva da API.

A classe deve representar **o mecanismo físico**, enquanto detalhes de hardware e parâmetros configuráveis devem ser mantidos separados quando isso trouxer benefício arquitetural real.

---

# 5. Avaliação de `Viper.java`

## 5.1 Situação atual

A classe não possui qualquer implementação.

Isso não constitui, por si só, um problema de arquitetura. Neste estágio, a existência da classe pode ser considerada apenas um **placeholder estrutural** para o futuro subsistema.

Entretanto, nenhuma conclusão sobre qualidade de controle, segurança ou comportamento do mecanismo pode ser obtida enquanto a implementação permanecer vazia.

---

## 5.2 Responsabilidades esperadas

Quando implementado, `Viper` deve concentrar as operações de alto nível do mecanismo.

A API futura deve ser orientada ao comportamento do mecanismo, e não simplesmente expor diretamente o hardware.

Por exemplo, dependendo do funcionamento real do Viper, uma API futura poderia possuir operações conceituais como:

```java
extend();
retract();
stop();
setTarget(...);
```

ou, caso o mecanismo seja baseado em posições:

```java
goToPosition(...);
holdPosition();
stop();
```

Esses exemplos são apenas direcionais. **Não devem ser implementados até que o comportamento mecânico real seja definido.**

A auditoria recomenda evitar uma API prematuramente baseada em detalhes específicos do motor, como:

```java
setMotorPower(...)
```

como única interface pública do subsistema.

O subsistema deve ser capaz de esconder detalhes de hardware quando esses detalhes não forem relevantes para quem o utiliza.

---

# 6. Avaliação de `ViperConfig.java`

## 6.1 Situação atual

`ViperConfig` é uma classe vazia.

Não há atualmente parâmetros configuráveis.

Isso significa que ainda não existe evidência suficiente para decidir quais constantes devem pertencer ao Viper.

---

## 6.2 Responsabilidade esperada

Caso o mecanismo possua parâmetros específicos, `ViperConfig` pode concentrar valores como:

* nomes de dispositivos;
* limites mecânicos;
* posições de referência;
* velocidades;
* potências máximas;
* tolerâncias;
* constantes de controle;
* parâmetros relacionados à calibração.

Entretanto, esses valores não devem ser adicionados simplesmente para preencher a classe.

A existência de `ViperConfig` deve ser justificada pela necessidade de separar **configuração do mecanismo** de **lógica de controle**.

---

# 7. HardwareMap e inicialização

Atualmente não existe acesso ao `HardwareMap`.

Consequentemente, não é possível avaliar:

* quais dispositivos compõem o Viper;
* nomes utilizados no `hardwareMap`;
* direção dos motores;
* modo de funcionamento;
* comportamento de zero power;
* encoder;
* limites físicos;
* sensores;
* sequência de inicialização.

A implementação futura deve centralizar a aquisição e configuração do hardware no próprio subsistema, evitando que OpModes ou Commands precisem conhecer diretamente os dispositivos físicos do Viper.

---

# 8. Segurança e limites

Nenhum mecanismo de proteção está implementado atualmente.

Dependendo da natureza física do Viper, deverão ser avaliados pelo menos:

* limite superior;
* limite inferior;
* posição inicial;
* prevenção de movimento além do curso mecânico;
* comportamento em ausência de referência;
* comportamento ao perder comando;
* potência/velocidade máxima;
* necessidade de encoder ou sensor de limite;
* comportamento durante inicialização.

Neste momento, **não é possível afirmar quais dessas proteções são necessárias**, pois a mecânica do Viper ainda não está descrita no código analisado.

---

# 9. Integração com Commands

Não há integração atualmente.

A arquitetura desejável é que Commands solicitem comportamentos ao subsistema Viper, em vez de manipular diretamente seus motores ou servos.

Fluxo esperado:

```text
Command
   ↓
Viper
   ↓
Hardware
```

e não:

```text
Command
   ↓
DcMotor / Servo
```

Isso mantém a responsabilidade pelo mecanismo dentro do subsystem e permite que a implementação de hardware seja modificada sem exigir alterações em todos os Commands consumidores.

---

# 10. Integração com OpModes

Não há integração identificável nos arquivos fornecidos.

Seguindo a diretriz estabelecida nas auditorias anteriores, os OpModes existentes devem ser tratados como **referência de uso do código atual**, e não como definição obrigatória da arquitetura final.

Para o Viper, a preferência arquitetural deve ser:

```text
OpMode
   ↓
Command / lógica de alto nível
   ↓
Viper
   ↓
Hardware
```

O OpMode não deve precisar conhecer detalhes como:

* qual motor controla o mecanismo;
* qual porta está sendo utilizada;
* qual potência corresponde a determinado comportamento;
* como os limites mecânicos são tratados.

---

# 11. Estado e controle

Nenhum modelo de estado está definido.

Antes da implementação, deve ser avaliado se o Viper precisa distinguir estados como:

```text
IDLE
EXTENDING
RETRACTING
HOLDING
AT_TARGET
ERROR
```

A lista acima é apenas ilustrativa.

Não se recomenda introduzir uma máquina de estados apenas por padrão arquitetural. Ela deve existir somente se o comportamento do mecanismo justificar essa complexidade.

---

# 12. Acoplamento

O código atual possui **acoplamento praticamente inexistente**, pois as classes não possuem dependências.

Isso é positivo do ponto de vista estrutural, mas decorre diretamente do fato de a implementação ainda não existir.

A implementação futura deve evitar acoplamento desnecessário com:

* OpModes;
* Commands específicos;
* outras subsystems;
* constantes globais sem relação direta com o mecanismo;
* lógica de jogo que deveria pertencer à camada de Commands.

---

# 13. Problemas identificados

### VPR-001 — Subsistema não implementado

**Severidade:** Informativo / Estrutural

`Viper` não possui comportamento ou acesso a hardware.

**Impacto:**

O subsistema ainda não pode executar nenhuma função do mecanismo.

**Recomendação:**

Definir primeiro a responsabilidade mecânica e, posteriormente, implementar a camada de hardware e a API comportamental.

---

### VPR-002 — Configuração inexistente

**Severidade:** Informativo / Estrutural

`ViperConfig` está vazio.

**Impacto:**

Ainda não existem parâmetros explicitamente associados ao mecanismo.

**Recomendação:**

Adicionar somente configurações que sejam efetivamente necessárias após a definição do hardware e do comportamento do Viper.

---

### VPR-003 — Responsabilidade mecânica não documentada

**Severidade:** Média

O código não documenta qual mecanismo físico é representado por `Viper`.

**Impacto:**

Não é possível determinar corretamente:

* hardware necessário;
* API;
* limites;
* modelo de controle;
* necessidade de sensores;
* comportamento de segurança.

**Recomendação:**

Documentar a função física do Viper antes da definição definitiva da implementação.

---

### VPR-004 — Ausência de contrato de API

**Severidade:** Informativo

Não existem métodos públicos que definam como outros componentes devem utilizar o Viper.

**Recomendação:**

Definir uma API orientada ao comportamento do mecanismo após a definição de seus requisitos.

---

# 14. Pontos positivos

Apesar da implementação estar incompleta, alguns aspectos estruturais são adequados:

* O Viper já possui um package próprio.
* O mecanismo possui uma classe de subsystem dedicada.
* Existe uma classe separada para configuração.
* Não há lógica de hardware espalhada.
* Não há dependência prematura de Commands ou OpModes.
* Não há abstrações artificiais ou complexidade desnecessária.

O estado atual é, portanto, melhor interpretado como **estrutura preparada para implementação**, e não como uma implementação problemática.

---

# 15. Direção recomendada

A implementação do Viper deve seguir aproximadamente esta sequência:

### Etapa 1 — Definir a mecânica

Documentar:

* função do mecanismo;
* componentes atuadores;
* sensores;
* curso;
* posições relevantes;
* comportamento esperado.

### Etapa 2 — Definir hardware

Identificar:

* motores;
* servos;
* sensores;
* nomes no `HardwareMap`;
* encoders;
* limites.

### Etapa 3 — Definir configuração

Adicionar ao `ViperConfig` apenas os parâmetros que precisam ser configuráveis.

### Etapa 4 — Implementar subsystem

Implementar:

* aquisição do hardware;
* configuração;
* estado interno;
* operações básicas;
* proteções necessárias.

### Etapa 5 — Definir API comportamental

Criar métodos que expressem as operações que o restante do robô precisa solicitar ao mecanismo.

### Etapa 6 — Integrar Commands

Criar Commands responsáveis por sequências ou ações de maior nível, mantendo o controle físico dentro do Viper.

### Etapa 7 — Validar com OpModes

Utilizar os OpModes existentes como referência operacional para verificar como o mecanismo é atualmente utilizado, sem assumir que essa estrutura de uso deverá necessariamente permanecer na arquitetura final.

---

# 16. Critérios para próxima revisão

A próxima auditoria do Viper deverá verificar:

* [ ] função mecânica documentada;
* [ ] hardware identificado;
* [ ] `HardwareMap` encapsulado;
* [ ] configuração definida;
* [ ] limites mecânicos definidos;
* [ ] comportamento de segurança definido;
* [ ] API pública definida;
* [ ] estado interno justificado;
* [ ] Commands integrados;
* [ ] ausência de acesso direto ao hardware fora do subsystem;
* [ ] parâmetros ajustáveis separados da lógica;
* [ ] comportamento validado em campo.

---

# 17. Conclusão

O Viper está atualmente em **estado inicial de estruturação**, composto por um subsystem vazio e uma classe de configuração vazia.

Não foram identificados bugs funcionais porque ainda não existe comportamento implementado para ser executado ou validado.

O principal ponto pendente não é uma correção de código, mas a **definição da responsabilidade e do comportamento físico do mecanismo**. A partir dessa definição, será possível estabelecer corretamente hardware, configuração, API, limites e integração com Commands.

**Status da AUD-005-Viper: BASELINE / AGUARDANDO IMPLEMENTAÇÃO**

Nenhuma refatoração de código é recomendada neste momento. A próxima ação deve ser levantar a especificação real do mecanismo Viper e, a partir dela, implementar o subsystem de forma incremental.
