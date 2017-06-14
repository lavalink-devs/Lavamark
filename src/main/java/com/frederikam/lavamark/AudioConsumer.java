package com.frederikam.lavamark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioConsumer extends Thread {

    private static final Logger log = LoggerFactory.getLogger(AudioConsumer.class);

    private final CopyOnWriteArrayList<Player> players;
    private final int INTERVAL = 20; // A frame is 20ms

    private AtomicInteger served = new AtomicInteger();
    private AtomicInteger missed = new AtomicInteger();
    private EndReason endReason = EndReason.NONE;
    private boolean running = true;

    AudioConsumer(CopyOnWriteArrayList<Player> players) {
        this.players = players;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        long i = 0;

        //noinspection InfiniteLoopStatement
        while (running) {

            //long countStart = System.currentTimeMillis();

            for (Player player :
                    players) {
                if (player.canProvide()) {
                    served.incrementAndGet();
                } else {
                    missed.incrementAndGet();
                }
            }

            long targetTime = ((start / INTERVAL) + i + 1) * INTERVAL;
            long diff = targetTime - System.currentTimeMillis();
            //log.info("Behind by " + (-diff) + "ms");
            i++;

            if(diff < -500) {
                endReason = EndReason.CANT_KEEP_UP;
                break;
            }

            //log.info("Time taken: " + (System.currentTimeMillis() - countStart));

            synchronized (this) {
                try {
                    if(diff > 0) {
                        sleep(diff);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    Results getResults() {
        int serv = served.getAndSet(0);
        int miss = missed.getAndSet(0);

        if((serv + miss) / 100 < miss) {
            endReason = EndReason.MISSED_FRAMES;
            running = false;
        }

        return new Results(serv, miss, endReason);
    }

    public class Results {
        int served;
        int missed;
        EndReason endReason;

        Results(int served, int missed, EndReason endReason) {
            this.served = served;
            this.missed = missed;
            this.endReason = endReason;
        }

        public int getServed() {
            return served;
        }

        public int getMissed() {
            return missed;
        }

        public EndReason getEndReason() {
            return endReason;
        }

        public String getLossPercentString() {
            //log.info("Miss " + missed);
            //log.info("Serv " + served);
            double frac = 1 - ((double) served) / ((double) (served + missed));
            frac = Math.floor(frac * 10000) / 10000;
            frac = frac * 100;
            return frac + "%";
        }
    }

    public enum EndReason {
        NONE,
        CANT_KEEP_UP,
        MISSED_FRAMES;
    }
}
