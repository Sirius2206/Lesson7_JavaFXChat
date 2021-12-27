package com.example.chat.ABCThread;

public class ABCThread {
    final Object let = new Object();
    volatile char currentLetter = 'A';
    public static void main(String[] args){
        ABCThread abcobj = new ABCThread();
        Thread prAThread = new Thread(abcobj::printA);
        Thread prBThread = new Thread(abcobj::printB);
        Thread prCThread = new Thread(abcobj::printC);
        prAThread.start();
        prBThread.start();
        prCThread.start();

    }

    public void printA() {
        synchronized (let) {
            try {
                for (int i = 0; i < 5; i++){
                    while (currentLetter != 'A') {
                        let.wait();
                    }
                    System.out.print(currentLetter);
                    currentLetter = 'B';
                    let.notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void printB() {
        synchronized (let) {
            try {
                for (int i = 0; i < 5; i++){
                    while (currentLetter != 'B') {
                        let.wait();
                    }
                    System.out.print(currentLetter);
                    currentLetter = 'C';
                    let.notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void printC() {
        synchronized (let) {
            try {
                for (int i = 0; i < 5; i++){
                    while (currentLetter != 'C') {
                        let.wait();
                    }
                    System.out.print(currentLetter);
                    currentLetter = 'A';
                    let.notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
