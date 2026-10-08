package org.firstinspires.ftc.teamcode.robot;

/**
 * Constantes GERAIS do robô e da estratégia. Configuração específica de um
 * componente pertence à Config do componente:
 *
 *   DrivetrainConfig, ShooterConfig, IntakeConfig, VisionConfig.
 *
 * Esta classe não deve voltar a crescer como depósito global.
 */
public class RConstants {

    // Sistemas presentes na versão completa do robô.
    // Para testes sem algum mecanismo, desative apenas temporariamente.
    // O Robot respeita estas flags: sistema desativado não é criado nem
    // inicializado, e o getter correspondente devolve null.
    public static final boolean USE_CAMERA = true;
    public static final boolean USE_SHOOTER = true;
    public static final boolean USE_FEEDER = true;
    public static final boolean USE_LOCALIZATION = true;

    // Observação: a torreta (CameraTurret) foi removida do robô.
    // O shooter agora é fixo. No TeleOp, a câmera fornece bearing e distância.
    // Nos autônomos, o chassi gira pela IMU e se posiciona pelos encoders;
    // a distância de cada posição de tiro é escrita diretamente na rota.

    // Limites gerais de potência do hardware
    public static final double MIN_MOTOR_POWER = -1.0;
    public static final double MAX_MOTOR_POWER = 1.0;

    // Evita que um pequeno ruído analógico dos gatilhos acione mecanismos.
    public static final double GAMEPAD_TRIGGER_THRESHOLD = 0.20;

    // Conversões
    public static final double INCH_TO_CM = 2.54;

    // ---------------------------------------------------------------------
    // AUTÔNOMOS - limites gerais e tempos de segurança
    // ---------------------------------------------------------------------
    public static final long AUTO_DRIVE_TIMEOUT_MS = 4000;
    public static final long AUTO_TURN_TIMEOUT_MS = 3000;
    public static final long AUTO_SHOOTER_READY_TIMEOUT_MS = 4000;
    public static final long AUTO_DELAY_BETWEEN_SHOTS_MS = 500;

    // Tempo máximo para uma alimentação individual e para o shooter recuperar
    // a velocidade depois que uma bolinha encosta nos rolos.
    public static final long AUTO_FEED_TIMEOUT_MS = 1500;
    public static final long AUTO_SHOOTER_RECOVERY_TIMEOUT_MS = 2500;

    // false evita lançar quando os motores não atingiram o RPM calibrado.
    public static final boolean AUTO_SHOOT_AFTER_RPM_TIMEOUT = false;

    // false evita usar automaticamente o último RPM da tabela quando a posição
    // de tiro ainda não foi calibrada.
    public static final boolean AUTO_ALLOW_DISTANCE_OUTSIDE_RPM_TABLE = false;

    // Tempo máximo (ms) que o autônomo espera o robô girar em direção ao alvo
    public static final long AIM_TURN_TIMEOUT_MS = 1500;
}
