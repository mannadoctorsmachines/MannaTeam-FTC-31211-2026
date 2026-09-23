package org.firstinspires.ftc.teamcode.robot;

public class RConstants {

    // Hardware Map - Sensores
    public static final String IMU = "imu";
    public static final String WEBCAM = "Webcam 1";

    // Hardware Map - Mecanismos
    public static final String SHOOTER_LEFT = "shooterMotor1";
    public static final String SHOOTER_RIGHT = "shooterMotor2";
    public static final String FEEDER = "intakeMotor";

    // Sistemas presentes na versão completa do robô.
    // Para testes sem algum mecanismo, desative apenas temporariamente.
    public static final boolean USE_CAMERA = true;
    public static final boolean USE_SHOOTER = true;
    public static final boolean USE_FEEDER = true;

    // Observação: a torreta (CameraTurret) foi removida do robô.
    // O shooter agora é fixo. No TeleOp, a câmera fornece bearing e distância.
    // Nos autônomos, o chassi gira pela IMU e se posiciona pelos encoders;
    // a distância de cada posição de tiro é escrita diretamente na rota.
    // A versão antiga com torreta continua arquivada em backups/CameraTurret.java
    // e também deve ser mantida numa branch/tag separada no GitHub.

    // Limites gerais
    public static final double MIN_MOTOR_POWER = -1.0;
    public static final double MAX_MOTOR_POWER = 1.0;

    // Mecanum normalmente escorrega mais de lado. Comece com este fator e
    // calibre separadamente comandando um strafe de 100 cm.
    public static final double STRAFE_CORRECTION = 1.15;

    // PID de giro do robô
    public static final double TURN_KP = 0.018;
    public static final double TURN_KI = 0.0;
    public static final double TURN_KD = 0.0015;
    public static final double TURN_MIN_POWER = 0.12;
    public static final double TURN_MAX_POWER = 0.45;

    // ---------------------------------------------------------------------
    // SHOOTER - todos os RPMs pedidos pelo código são RPM DO ROLO
    // ---------------------------------------------------------------------
    // Estes três valores precisam ser confirmados antes da calibração final.
    //
    // 1) Ticks registrados pelo encoder em uma volta do eixo de saída do motor.
    //    Obtenha no datasheet do modelo exato do motor.
    public static final double SHOOTER_MOTOR_TICKS_PER_REV = 537.7;

    // 2) RPM nominal do eixo de saída do motor, informado pelo fabricante.
    //    Zero significa "ainda não informado" e gera aviso na telemetria.
    public static final double SHOOTER_MOTOR_NOMINAL_RPM = 6000.0;

    // 3) Quantas voltas o motor dá para o rolo completar uma volta.
    //    Com correia dentada:
    //    dentes da polia do rolo / dentes da polia do motor.
    //    Direto no mesmo eixo = 1.0.
    public static final double SHOOTER_MOTOR_REVS_PER_ROLLER_REV = 1.0;

    // Valor efetivamente usado na conversão entre ticks/s e RPM do rolo.
    public static final double SHOOTER_TICKS_PER_ROLLER_REV =
            SHOOTER_MOTOR_TICKS_PER_REV
                    * SHOOTER_MOTOR_REVS_PER_ROLLER_REV;

    // Também são RPM do rolo. MAX_SHOOTER_RPM nunca deve ser maior que
    // SHOOTER_MOTOR_NOMINAL_RPM / SHOOTER_MOTOR_REVS_PER_ROLLER_REV.
    public static final double MIN_SHOOTER_RPM = 300.0;
    public static final double MAX_SHOOTER_RPM = 3000.0;

    // =====================================================================
    // >>> AJUSTE AQUI O RPM DO SHOOTER NORMAL / MANUAL (gamepad2 LB) <<<
    // =====================================================================
    // RPM DO ROLO usado pelo shooter manual (gamepad2 LB).
    public static final double MANUAL_SHOOTER_RPM = 20.0;

    // Fallback usado apenas se a tabela de distância estiver inválida.
    // Mantê-lo ligado ao RPM manual evita dois valores padrão diferentes.
    public static final double DEFAULT_SHOOTER_RPM = MANUAL_SHOOTER_RPM;

    // Passos usados no OpMode "DECODE Calibrar Distancia e RPM".
    public static final double SHOOTER_CALIBRATION_COARSE_STEP_RPM = 100.0;
    public static final double SHOOTER_CALIBRATION_FINE_STEP_RPM = 25.0;

    public static final double SHOOTER_READY_TOLERANCE_RPM = 85.0;

    // Ao ligar o shooter manual ou automático, o intake só é liberado
    // depois deste tempo mínimo de aceleração.
    public static final long SHOOTER_SPINUP_DELAY_MS = 2000;

    // Intake / Feeder
    public static final double INTAKE_REST_POWER = 0.0;
    public static final double INTAKE_PUSH_POWER = 0.75;
    public static final double INTAKE_REVERSE_POWER = -0.75;
    public static final long INTAKE_PUSH_TIME_MS = 260;

    // Evita que um pequeno ruído analógico dos gatilhos acione mecanismos.
    public static final double GAMEPAD_TRIGGER_THRESHOLD = 0.20;

    // Conversões
    public static final double INCH_TO_CM = 2.54;

    // Cálculo de distância do shooter (range da AprilTag medido pela câmera).
    // CAMERA_TO_SHOOTER_OFFSET_CM corrige a diferença entre a câmera e o ponto
    // de saída da bolinha. Use positivo se o shooter estiver mais longe do alvo
    // que a câmera e negativo se estiver mais perto.
    public static final double CAMERA_TO_SHOOTER_OFFSET_CM = -26.5; // 12.0 valor antigo
    public static final double TAG_TO_TARGET_OFFSET_CM = 0.0;

    // Suaviza pequenas oscilações da câmera. 1.0 = sem filtro; valores menores
    // deixam a leitura mais estável, porém um pouco mais lenta para responder.
    public static final double CAMERA_DISTANCE_FILTER_ALPHA = 0.5; //0.25 valor antigo

    public static final double TURN_TOLERANCE_DEGREES = 2.0;

    // ID da AprilTag colocada no gol. Use -1 para aceitar qualquer tag visível.
    public static final int TARGET_APRIL_TAG_ID = -1;
    public static final long CAMERA_SOLUTION_MAX_AGE_MS = 500;

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

    // Mira
    public static final double AIM_TOLERANCE_DEGREES = 2.0;

    // Tempo máximo (ms) que o autônomo espera o robô girar em direção ao alvo
    public static final long AIM_TURN_TIMEOUT_MS = 1500;

    // Correção encontrada nos testes físicos da webcam.
    public static final double CAMERA_DISTANCE_SCALE = 0.78125;
    public static final double CAMERA_DISTANCE_BIAS_CM = 0.0;


}
