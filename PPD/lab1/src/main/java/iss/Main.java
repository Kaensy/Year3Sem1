package iss;

import java.util.Random;

public class Main {

    public static class Thread1 extends Thread {
        private int id, p, n;
        private int[] A, B, C;

        public Thread1(int id, int p, int n, int[] a, int[] b, int[] c) {
            this.id = id;
            this.p = p;
            this.n = n;
            A = a;
            B = b;
            C = c;
        }

        public void run() {
            for (int i = 0; i < n; i++) {
                C[i] = A[i] + B[i];
            }
        }
    }

    public static void main(String[] args) {
        int n = 963, max = 50;
        int[] A = generator(n, max);
        int[] B = generator(n, max);

        int[] C = new int[n];
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            C[i] = A[i] + B[i];
        }
        long end = System.currentTimeMillis();
        System.out.println("Durata1: ");
        System.out.println(end - start);
        printVectors(C);

        int[] C2 = new int[n];
        int p = 10;
        Thread1[] threads = new Thread1[n];
        long stratThread = System.currentTimeMillis();
        for (int i = 0; i < p; i++) {
            Thread1 thread = new Thread1(i, p, n, A, B, C2);
            threads[i] = thread;
            thread.start();
        }

        for (int i = 0; i < p; i++) {
            try {
                threads[i].join();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        long endThread = System.currentTimeMillis();
        System.out.println("Durata2: ");
        System.out.println(endThread - stratThread);
        printVectors(C2);

        stratThread = System.currentTimeMillis();
        int[] arr5 = new int[n];
        Thread2[] threads2 = new Thread2[p];

        for (int i = 0; i < p; i++) {
            int interval = n / p;
            int startThreadInterval = i * interval;
            int endThreadInterval = (i == p - 1) ? n - 1 : (i + 1) * interval - 1;
            Thread2 thread = new Thread2(startThreadInterval, endThreadInterval, A, B, arr5);
            threads2[i] = thread;
            thread.start();
        }

        for (int i = 0; i < p; i++) {
            try {
                threads2[i].join();
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted: " + e.getMessage());
            }
        }

        long endThread2 = System.currentTimeMillis();
        System.out.println("Durata3: ");
        System.out.println(endThread2 - stratThread);
        printVectors(arr5);
    }

    private static void printVectors(int[] c) {
        for (int i = 0; i < c.length; i++) {
            System.out.print(c[i] + " ");
        }
        System.out.println();
    }

    private static int[] generator(int n, int max) {
        int[] vector = new int[n];
        Random random = new Random();
        for (int i = 0; i < n; i++) {
            vector[i] = random.nextInt(max);
        }
        return vector;
    }

    public static class Thread2 extends Thread {
        private int start, end;
        private int[] A, B, C;

        public Thread2(int start, int end, int[] a, int[] b, int[] c) {
            this.start = start;
            this.end = end;
            A = a;
            B = b;
            C = c;
        }

        public void run() {
            for (int i = start; i <= end; i++) {
                C[i] = A[i] + B[i];
            }
        }
    }
}
