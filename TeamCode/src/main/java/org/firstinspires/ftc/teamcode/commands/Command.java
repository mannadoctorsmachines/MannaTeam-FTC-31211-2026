package org.firstinspires.ftc.teamcode.commands;

/**
 * Uma ação do robô com ciclo de vida definido.
 *
 *   initialize()  -> uma vez, ao iniciar
 *   execute()     -> repetidamente, enquanto isFinished() for false
 *   isFinished()  -> true quando a ação terminou
 *   end(interrupted) -> uma vez, ao terminar ou ao ser cancelada
 *
 * Um Command coordena Subsystems recebidos pelo construtor e NUNCA acessa
 * hardware diretamente. Commands específicos de uma temporada (ex.: pontuar)
 * ficam fora da base universal.
 *
 * Em end(true) o Command deve deixar os subsistemas em estado seguro.
 */
public abstract class Command {

    private long startTimeMs = 0;

    public void initialize() {
    }

    public abstract void execute();

    public abstract boolean isFinished();

    public void end(boolean interrupted) {
    }

    /** Tempo máximo de execução em ms. Zero ou negativo = sem limite. */
    public long getTimeoutMs() {
        return 0;
    }

    final void markStarted() {
        startTimeMs = System.currentTimeMillis();
    }

    final boolean hasTimedOut() {
        long timeoutMs = getTimeoutMs();

        return timeoutMs > 0
                && System.currentTimeMillis() - startTimeMs >= timeoutMs;
    }
}
