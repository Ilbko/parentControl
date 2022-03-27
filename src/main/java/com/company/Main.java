package com.company;

import java.util.Scanner;

public class Main {

    static TextInputObject ti;
    static TextOutputObject to;

    public static void main(String[] args) {
        //we instantiate the objects
        ti = new TextInputObject();
        to = new TextOutputObject();
        //we call the start method to start the threads for input and output
        ti.start();
        to.start();
    }

    //TextInputObject class
    public static class TextInputObject implements Runnable {

        //Method that gets called when the object is instantiated
        public TextInputObject() {
            System.out.println("Created TextInputObject");
        }

        //create a thread object and check if it's not already created
        static Thread thread;

        //This method gets called from the main
        public void start() {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }

        //this method gets called by the thread.start(); from above
        @Override
        public void run() {
            System.out.println("Text input thread created and now it runs");

            readTextFromConsole();
        }

        Scanner inputReader = new Scanner(System.in);

        //check for input all the time - THIS WILL NOT HALT THE PROGRAM
        public void readTextFromConsole() {
            //System.out.println("Enter something:");
            TextOutputObject.output = inputReader.nextLine();
            //String myinput = inputReader.nextLine();
            //System.out.println("You Entered: " + myinput);
            readTextFromConsole();
        }

    }


    //TextOutputObject
    public static class TextOutputObject implements Runnable {

        static String output = "test";
        //Method that gets called when the object is instantiated
        public TextOutputObject() {
            System.out.println("Created TextOutputObject");
        }

        static Thread thread;

        public void start() {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }

        @Override
        public void run() {
            System.out.println("Text output thread created and now it runs");

            //Make it output text every 4 seconds to test if you can input text while it's used for output
            while (true) {
                if (output != "") {
                    System.out.println(output);
                    output = "";
                }
            }
        }
    }
}
