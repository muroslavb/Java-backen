package com.example.demo.async;

final class Delays {

    private Delays() {
    }

    static void pause(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Виконання перервано", ex);
        }
    }
}
