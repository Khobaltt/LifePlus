/*
 * Running:
 * javac -d out src/main/java/Main.java src/main/java/core/*.java src/main/java/ui/*.java
 * java -cp out main.java.Main
 */

package main.java;

import main.java.ui.MainWindow;

public class Main {

    public static void main(String[] args) {
        MainWindow.launch();
    }
}
