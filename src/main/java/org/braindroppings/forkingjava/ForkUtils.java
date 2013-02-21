package org.braindroppings.forkingjava;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class ForkUtils {

    public static void pipe(final InputStream src, final PrintStream dest) {
        new Thread(new Runnable() {
            public void run() {
                int n;
                try {
                    while ((n = src.available()) > 0) {
                        byte[] b = new byte[n];
                        n = src.read(b);
                        if (n > 0) {
                            dest.write(b, 0, n);
                        }
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
