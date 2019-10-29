package owlsmellplugin.main;

import static org.semanticweb.owlapi.search.Searcher.annotationObjects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ui.PartInitException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import owlsmellplugin.util.ConsoleUtil;

public class OWLSmellHelper {

	private static final String URL = "http://www.semanticweb.org/ontology_smell#";
	
	// change the path to load the file correctly
	private static final String PATH = "C:\\ontology_smell.owl";

	private static OWLSmellHelper instance;

	private OWLOntologyManager manager;
	private File file;
	private OWLOntology ontology;
	private OWLDataFactory factory;
	private OWLObjectRenderer renderer = new DLSyntaxObjectRenderer();

	private OWLSmellHelper() {
		
	}

	public static OWLSmellHelper getInstance() {
		if (instance == null)
			instance = new OWLSmellHelper();
		
		instance.initOntology();

		return instance;
	}
	
	private void initOntology() {
		loadOntology();
		removeAllIndividual();
	}

	private void loadOntology() {
		try {
			manager = OWLManager.createOWLOntologyManager();
			file = new File(PATH);
			ontology = manager.loadOntologyFromOntologyDocument(file);
			factory = ontology.getOWLOntologyManager().getOWLDataFactory();
			System.out.println("Ontology loaded!");
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

	private void removeAllIndividual() {
		try {
			System.out.println("Removing individuals...");
			Set<OWLClass> classes = ontology.getClassesInSignature();
			OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
			OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
			Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
			ontologies.add(ontology);
			OWLEntityRemover remover = new OWLEntityRemover(ontologies);
			for (OWLClass clazz : classes) {
				for (OWLNamedIndividual individual : reasoner.getInstances(clazz, false).getFlattened()) {
					individual.accept(remover);
				}
			}
			manager.applyChanges(remover.getChanges());
			manager.saveOntology(ontology);
			System.out.println("Individuals removed successfully!");
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}

	public void createIndividualByClass(String individualName, OWLSmellClass owlSmellClass) {
		try {
			OWLClass owlClass = factory.getOWLClass(IRI.create(URL + owlSmellClass.name()));
			OWLIndividual individual = factory.getOWLNamedIndividual(IRI.create(URL + individualName));
			OWLClassAssertionAxiom classAssertionAxiom = factory.getOWLClassAssertionAxiom(owlClass, individual);
			AddAxiom addAxiom = new AddAxiom(ontology, classAssertionAxiom);
			manager.applyChange(addAxiom);
			manager.saveOntology(ontology);
//			System.out.println(individualName + " created!");
			
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}
	
	public void makeAllIndividualsDifferent() {
		try {
			OWLDifferentIndividualsAxiom diff = factory.getOWLDifferentIndividualsAxiom(
					ontology.getIndividualsInSignature(Imports.EXCLUDED));
			AddAxiom addAxiom = new AddAxiom(ontology, diff);
			manager.applyChange(addAxiom);
			manager.saveOntology(ontology);
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}

	public void associateDataPropertyToIndividual(OWLSmellDataProperty owlSmellAxiom, String dataPropertyValue,
			String individualName) {
		try {
			OWLDataProperty dataProperty = factory.getOWLDataProperty(IRI.create(URL + owlSmellAxiom.name()));
			OWLIndividual individual = factory.getOWLNamedIndividual(IRI.create(URL + individualName));
			OWLDataPropertyAssertionAxiom dataAxiom = factory.getOWLDataPropertyAssertionAxiom(dataProperty, individual,
					dataPropertyValue);
			AddAxiom addAxiom = new AddAxiom(ontology, dataAxiom);
			manager.applyChange(addAxiom);
			manager.saveOntology(ontology);
//			System.out.println(
//					owlSmellAxiom + " with value " + dataPropertyValue + " associated in " + individualName + "!");
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}

	public void associateDataPropertyToIndividual(OWLSmellDataProperty owlSmellAxiom, int dataPropertyValue,
			String individualName) {
		try {
			OWLDataProperty dataProperty = factory.getOWLDataProperty(IRI.create(URL + owlSmellAxiom.name()));
			OWLIndividual individual = factory.getOWLNamedIndividual(IRI.create(URL + individualName));
			OWLDataPropertyAssertionAxiom dataAxiom = factory.getOWLDataPropertyAssertionAxiom(dataProperty, individual,
					dataPropertyValue);
			AddAxiom addAxiom = new AddAxiom(ontology, dataAxiom);
			manager.applyChange(addAxiom);
			manager.saveOntology(ontology);
//			System.out.println(
//					owlSmellAxiom + " with value " + dataPropertyValue + " associated in " + individualName + "!");
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}
	
	public void associateDataPropertyToIndividual(OWLSmellDataProperty owlSmellAxiom, boolean dataPropertyValue,
			String individualName) {
		try {
			OWLDataProperty dataProperty = factory.getOWLDataProperty(IRI.create(URL + owlSmellAxiom.name()));
			OWLIndividual individual = factory.getOWLNamedIndividual(IRI.create(URL + individualName));
			OWLDataPropertyAssertionAxiom dataAxiom = factory.getOWLDataPropertyAssertionAxiom(dataProperty, individual,
					dataPropertyValue);
			AddAxiom addAxiom = new AddAxiom(ontology, dataAxiom);
			manager.applyChange(addAxiom);
			manager.saveOntology(ontology);
//			System.out.println(
//					owlSmellAxiom + " with value " + dataPropertyValue + " associated in " + individualName + "!");
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}

	public void associateObjectPropertyToIndividuals(OWLSmellObjectProperty owlSmellAxiom, String individualName1,
			String individualName2) {
		try {
			OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create(URL + owlSmellAxiom.name()));
			OWLIndividual individual1 = factory.getOWLNamedIndividual(IRI.create(URL + individualName1));
			OWLIndividual individual2 = factory.getOWLNamedIndividual(IRI.create(URL + individualName2));
			OWLObjectPropertyAssertionAxiom objectAxiom = factory.getOWLObjectPropertyAssertionAxiom(objectProperty,
					individual1, individual2);
			AddAxiom addAxiom = new AddAxiom(ontology, objectAxiom);
			manager.applyChange(addAxiom);
			manager.saveOntology(ontology);
//			System.out.println(individualName1 + " " + owlSmellAxiom.name() + " " + individualName2 + " associated!");
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}

	public void verifyExistSomeSmell() {
		String message = "";
		PrefixManager pm = (PrefixManager) manager.getOntologyFormat(ontology);
		pm.setDefaultPrefix(URL);
		OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
		OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
		OWLClass classOO = factory.getOWLClass("GeneralType", pm);
		OWLClass smell = factory.getOWLClass("Smell", pm);
		Map<String, String> smells = new HashMap<>();
//		List<String> refactorMethod = new ArrayList();
		Map<String, Integer> map = new HashMap<>();
		int count = 0;
		
		OWLAnnotationProperty comment = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI());
		String strComment = "";
		
		// get annotations in owl class
//		for (OWLClass cls : ontology.getClassesInSignature()) {
//            // Get the annotations on the class that use the label property
//            for (OWLOntology o : ontology.getImportsClosure()) {
//                for (OWLAnnotation annotation : annotationObjects(o.getAnnotationAssertionAxioms(cls.getIRI()),
//                    label)) {
//                    if (annotation.getValue() instanceof OWLLiteral) {
//                        OWLLiteral val = (OWLLiteral) annotation.getValue();
//                             System.out.println(cls + " -> " + val.getLiteral());
//                    }
//                }
//            }
//        }

		for (OWLClass subClassOfSmell : reasoner.getSubClasses(smell, true).getFlattened()) {
//			System.out.println(subClassOfSmell.toString() + " - " + renderer.render(subClassOfSmell));
//			smells.put(renderer.render(subClassOfSmell));
			
			for (OWLOntology o : ontology.getImportsClosure()) {
                for (OWLAnnotation annotation : annotationObjects(o.getAnnotationAssertionAxioms(subClassOfSmell.getIRI()),
                    comment)) {
                    if (annotation.getValue() instanceof OWLLiteral) {
                        OWLLiteral val = (OWLLiteral) annotation.getValue();
                        strComment = val.getLiteral();
//                             System.out.println(renderer.render(subClassOfSmell) + " -> " + val.getLiteral());
                    }
                }
            }
			
			smells.put(renderer.render(subClassOfSmell), strComment);
			strComment = "";
		}
		
		String refactor = "";

		for (OWLNamedIndividual individual : reasoner.getInstances(classOO, false).getFlattened()) {
			String name = renderer.render(individual);

			for (String s : smells.keySet()) {
				OWLClass chOMPClass = factory.getOWLClass(s, pm);
				OWLClassAssertionAxiom axiomToExplain = factory.getOWLClassAssertionAxiom(chOMPClass, individual);
				if (reasoner.isEntailed(axiomToExplain)) {
					count++;
					message += count + " - " + name + " is a " + s + "\n";
					getSmellsCount(map, s);
					
					if (!refactor.contains(s))
						refactor += (s + ": " + smells.get(s) + "\n");
				}
			}
//			message+="\n";
		}

		if (smells.size() > 0) {
			message += "\n\nHow to refactor the identified smells?\n\n" + refactor;
		} else
			message = "No smells were found!";

//		message = message + "\n\n\n" + map.toString();
		
		System.out.println(message);
		try {
			ConsoleUtil.writeInConsole(message);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}
	
	private void getSmellsCount(Map<String, Integer> map, String smell) {
		int count = 0;
		
		if (map.containsKey(smell)) {
			count = map.get(smell) + 1;
			map.put(smell, count);
		} else {
			count++;
			map.put(smell, count);
		}
		count = 0;
	}

	
}
