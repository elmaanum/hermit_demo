package com.elmaanum.hermit;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;

public class Demo {
    private static final String PIZZA = "/pizza.owl";

    public static void main(String[] args) throws Exception {
        // First, we create an OWLOntologyManager object. The manager will load and save ontologies.
        final OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        // We use the OWL API to load the Pizza ontology.
        final OWLOntology ontology = ontologyManager.loadOntologyFromOntologyDocument(IRI.create(new File(PIZZA).toURI().toURL()));
        // Now, we instantiate HermiT by creating an instance of the Reasoner class in the package org.semanticweb.HermiT.
        final Reasoner hermit = new Reasoner(ontology);
        // Finally, we output whether the ontology is consistent.
        System.out.println(hermit.isConsistent());
    }
}
