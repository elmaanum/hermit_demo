package com.elmaanum.hermit;


/*
 * This Java source file was generated by the Gradle 'init' task.
 */
public class App {
    public String getGreeting() {
        return "Hello world.";
    }

    public static void main(String[] args) throws Exception {
        System.out.println(new App().getGreeting());
        // String[] rdf4jargs = new String[2];
        // rdf4jargs[0] = "dev";
        // rdf4jargs[1] = "recreate";

        new RDF4J().main(args);


    }
}
