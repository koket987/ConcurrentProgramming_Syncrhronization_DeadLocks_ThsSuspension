package edu.eci.arsw.highlandersim;

import java.util.List;
import java.util.Random;

public class Immortal extends Thread {

    private ImmortalUpdateReportCallback updateCallback=null;
    
    private int health;
    
    private int defaultDamageValue;

    private final List<Immortal> immortalsPopulation;

    private final String name;

    private static volatile boolean paused = false;

    private static final Object pauseLock = new Object();

    private final Random r = new Random(System.currentTimeMillis());


    public Immortal(String name, List<Immortal> immortalsPopulation, int health, int defaultDamageValue, ImmortalUpdateReportCallback ucb) {
        super(name);
        this.updateCallback=ucb;
        this.name = name;
        this.immortalsPopulation = immortalsPopulation;
        this.health = health;
        this.defaultDamageValue=defaultDamageValue;
    }

    public void run() {
        while (true) {
            synchronized (pauseLock) {
                while (paused) {
                    try {
                        pauseLock.wait(); // Esperar hasta que se reanude
                    } catch (InterruptedException e) {
                        return; // Salir si se interrumpe
                    }
                }
            }

            // Lógica de pelea...
            if (immortalsPopulation.size() > 1) {
                int myIndex = immortalsPopulation.indexOf(this);
                int nextFighterIndex = r.nextInt(immortalsPopulation.size());

                // Evitar pelear contra uno mismo
                if (nextFighterIndex == myIndex) {
                    nextFighterIndex = (nextFighterIndex + 1) % immortalsPopulation.size();
                }

                Immortal opponent = immortalsPopulation.get(nextFighterIndex);
                this.fight(opponent);
            } else {
                // Notificar que hay un ganador
                if (immortalsPopulation.size() == 1) {
                    ControlFrame.notifyWinner(this); // Notificar al ControlFrame
                }
                break; // Salir del bucle si no hay más peleas posibles
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // Manejar la interrupción y salir del bucle
                return; // Salir del hilo si se interrumpe
            }
        }
    }

    public static void pause() {
        paused = true;
    }

    public static void resumeInmortal() {
        paused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll(); // Notify all waiting threads to continue
        }
    }

    public void fight(Immortal i2) {
        synchronized (immortalsPopulation) {
            if (i2.getHealth() > 0) {
                i2.changeHealth(i2.getHealth() - defaultDamageValue);
                this.health += defaultDamageValue;
                updateCallback.processReport("Fight: " + this + " vs " + i2 + "\n");
            } else {
                // Remove the immortal from the population
                immortalsPopulation.remove(i2);
                updateCallback.processReport(i2 + " has been removed from the game.\n");
            }
        }
    }

    public void changeHealth(int v) {
        health = v;
    }

    public int getHealth() {
        return health;
    }

    @Override
    public String toString() {

        return name + "[" + health + "]";
    }

}
