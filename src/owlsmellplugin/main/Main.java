package owlsmellplugin.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.Document;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class Main extends AbstractHandler {

	private static final String JDT_NATURE = "org.eclipse.jdt.core.javanature";

//	private String text = "";
	private String parents = "";
	private OWLSmellHelper helper;
	private Map<String, String> mapOfParents;
	private int dit = 1;
	private int countClass = 0;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		helper = OWLSmellHelper.getInstance();
		mapOfParents = new HashMap();
		countClass = 0;
		
		getProjectInformation();
		
		return null;
	}

	private void getProjectInformation() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage activePage = window.getActivePage();

		IEditorPart activeEditor = activePage.getActiveEditor();

		if (activeEditor != null) {
			IEditorInput input = activeEditor.getEditorInput();

			IProject project = input.getAdapter(IProject.class);
			if (project == null) {
				IResource resource = input.getAdapter(IResource.class);
				if (resource != null) {
					project = resource.getProject();
					try {
						if (project.isOpen() && project.isNatureEnabled(JDT_NATURE)) {
							analyseMethods(project);
						} else {
							System.out.println("project is not open or its nature is not jdt_nature");
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("resource is null");
				}
			} else {
				System.out.println("project is not null");
			}
		} else {
			System.out.println("active editor is null");
		}

//        IWorkspace workspace = ResourcesPlugin.getWorkspace();
//        IWorkspaceRoot root = workspace.getRoot();
//        // Get all projects in the workspace
//        IProject[] projects = root.getProjects();
//        // Loop over all projects
//        for (IProject project : projects) {
//        	System.out.println(project.getLocation().toFile().getAbsolutePath() + "\n");
//            try {
//                if (project.isOpen() && project.isNatureEnabled(JDT_NATURE)) {
//                    analyseMethods(project);
//                }
//            } catch (CoreException e) {
//                e.printStackTrace();
//            }
//        }
	}

	private void analyseMethods(IProject project) throws JavaModelException {
		IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();
		// parse(JavaCore.create(project));
		for (IPackageFragment mypackage : packages) {
			if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
//				System.out.println("Package " + mypackage.getElementName());
//				text += "Package " + mypackage.getElementName() + "\n";
				createAST(mypackage);
			}

		}
//		System.out.println("\n\n\n");
		verifyNumberOfSons();
		helper.makeAllIndividualsDifferent();
		helper.verifyExistSomeSmell();
		System.out.println("Finished!");
	}

	private void createAST(IPackageFragment mypackage) throws JavaModelException {
		for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
			// now create the AST for the ICompilationUnits
			CompilationUnit parse = parse(unit);
			dit = getDIT(parse);
			printCompilationUnitDetails(mypackage, unit);
//			System.out.println("DEPTH OF INHERITANCE: " + dit);
			dit = 1;
			parents = "";
		}
	}

	private int getDIT(CompilationUnit unit) {
		for (Object type : unit.types()) {
			AbstractTypeDeclaration typeDec = (AbstractTypeDeclaration) type;
			ITypeBinding binding = typeDec.resolveBinding();
			if (binding != null) {
				getSuperClass(binding);
			}
		}
		return dit;
	}

	private void getSuperClass(ITypeBinding binding) {
		ITypeBinding father = binding.getSuperclass();
		if (father != null) {
			String fatherName = father.getQualifiedName();
			if (fatherName.equals("Object"))
				return;
			
			dit++;
			getSuperClass(father);
		}
	}

	private void printCompilationUnitDetails(IPackageFragment mypackage, ICompilationUnit unit) throws JavaModelException {
		OWLSmellClass classType;
		OWLSmellDataProperty dataPropertyName;
		OWLSmellDataProperty dataPropertyDIT;
		
		for (IType type : unit.getTypes()) {
			String className = type.getElementName();
			String packageName = mypackage.getElementName();
			countClass++;
			System.out.println(countClass + " - Class name: " + packageName + "." + className);
			
			if (Flags.isAbstract(type.getFlags())) {
				classType = OWLSmellClass.AbstractClassOO;
				dataPropertyName = OWLSmellDataProperty.clName;
				dataPropertyDIT = OWLSmellDataProperty.clDepthOfInheritance;
			} else if (Flags.isInterface(type.getFlags())) {
				classType = OWLSmellClass.Interface;
				dataPropertyName = OWLSmellDataProperty.inName;
				dataPropertyDIT = OWLSmellDataProperty.inDepthOfInheritance;
			} else if (Flags.isEnum(type.getFlags())) {
				classType = OWLSmellClass.Enum;
				dataPropertyName = OWLSmellDataProperty.enumName;
				dataPropertyDIT = OWLSmellDataProperty.clDepthOfInheritance;
			} else {
				classType = OWLSmellClass.ClassOO;
				dataPropertyName = OWLSmellDataProperty.clName;
				dataPropertyDIT = OWLSmellDataProperty.clDepthOfInheritance;
			}
			
			helper.createIndividualByClass(packageName + "." + className, classType);
			helper.associateDataPropertyToIndividual(dataPropertyName, className, packageName + "." + className);
			helper.associateDataPropertyToIndividual(dataPropertyDIT, dit, packageName + "." + className);
			printMethodsInformation(packageName + "." + className, type);
			printVariablesInformation(packageName + "." + className, unit, type.getFlags());
			
			String superClassName = type.getSuperclassName() != null ?
					packageName + "." + type.getSuperclassName() : "Object";
					
			helper.associateObjectPropertyToIndividuals(OWLSmellObjectProperty.hasSuper, packageName + "." + className, superClassName);
			
			if (Flags.isInterface(type.getFlags())) {
				className = className + "I";
			}
			mapOfParents.put(packageName + "." + className, superClassName);
			
			for (String interfaceName : type.getSuperInterfaceNames()) {
				helper.associateObjectPropertyToIndividuals(OWLSmellObjectProperty.implementss, packageName + "." + className, packageName + "." + interfaceName);
			}
			
		}
	}

	private void printMethodsInformation(String className, IType type) {
		int methodsCount = 0;
		int parametersCount = 0;
		
		try {
			methodsCount = type.getMethods().length;
			for (IMethod method : type.getMethods()) {
				String methodName = method.getElementName().toString();
				String source = method.getSource();
				String comment = (source.contains("/*") && source.contains("*/"))? source.substring(source.indexOf("/*"), source.indexOf("*/")) : "";
				Document doc = new Document(source);
				
				if (source.contains(comment))
					source = source.replace(comment, "").replace("*/", "");
				
				doc = new Document(source);
				
//				System.out.println("Method name: " + methodName);
//				System.out.println("Method body: \n" + source + "\n");
				
				parametersCount = method.getParameters().length;
//				System.out.println("Method count: " + parametersCount);
				
				helper.createIndividualByClass(className + "." + methodName, OWLSmellClass.Method);
				helper.associateObjectPropertyToIndividuals(OWLSmellObjectProperty.hasMethod, 
						className, className + "." + methodName);
				helper.associateDataPropertyToIndividual(OWLSmellDataProperty.mtName, methodName, className + "." + methodName);
				helper.associateDataPropertyToIndividual(OWLSmellDataProperty.mtNumberOfLines, doc.getNumberOfLines(), className + "." + methodName);
				helper.associateDataPropertyToIndividual(OWLSmellDataProperty.mtNumberParameters, parametersCount, className + "." + methodName);
				
				for (ILocalVariable parameter : method.getParameters()) {
//					System.out.println(getParameterType(parameter.getTypeSignature()) + " " + parameter.getElementName());
					
					String parameterName = parameter.getElementName();
					String parameterType = getParameterType(parameter.getTypeSignature());
					
					helper.createIndividualByClass(className + "." + methodName + "." + parameterName, OWLSmellClass.Parameter);
					helper.associateDataPropertyToIndividual(OWLSmellDataProperty.pmName, 
							methodName + "." + parameterName, className + "." + methodName + "." + parameterName);
					helper.associateDataPropertyToIndividual(OWLSmellDataProperty.pmType, 
							parameterType, className + "." + methodName + "." + parameterName);
					helper.associateObjectPropertyToIndividuals(OWLSmellObjectProperty.hasParameter, 
							className + "." + methodName, className + "." + methodName + "." + parameterName);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		OWLSmellDataProperty dataProperty;
		
		int flags = 0;
		try {
			flags = type.getFlags();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		if (!Flags.isEnum(flags)) {
			if (Flags.isAbstract(flags)) {
				dataProperty = OWLSmellDataProperty.clNumberOfMethods;
			} else if (Flags.isInterface(flags)) {
				dataProperty = OWLSmellDataProperty.inNumberOfMethods;
			} else {
				dataProperty = OWLSmellDataProperty.clNumberOfMethods;
			}
			
			helper.associateDataPropertyToIndividual(dataProperty, methodsCount, 
					className);
		}
	}

	private void printVariablesInformation(String className, ICompilationUnit unit, int flags) {
		CompilationUnit parse = parse(unit);
		VariableVisitor visitor = new VariableVisitor();
		parse.accept(visitor);

		for (FieldDeclaration variable : visitor.getVariables()) {
			List<VariableDeclarationFragment> fragments = variable.fragments();

			for (VariableDeclarationFragment fragment : fragments) {
				String varName = className + "." + fragment.resolveBinding().getName().toString();
				String varArray[] = fragment.resolveBinding().toString().split(" ");
				String varType = varArray[varArray.length-2].replace("<", "(").replace(">", ")").replace("''", "");
				
				helper.createIndividualByClass(varName, OWLSmellClass.NormalAttribute);
				helper.associateDataPropertyToIndividual(OWLSmellDataProperty.atAccessType, 
						getEncapsulation(variable.getModifiers()), varName);
				helper.associateObjectPropertyToIndividuals(OWLSmellObjectProperty.hasAttribute, 
						className, varName);
				helper.createIndividualByClass(varType, getOwlSmellClassByAttributeType(varType));
				helper.associateObjectPropertyToIndividuals(OWLSmellObjectProperty.isTypeOf, 
						varName, varType);
				
			}
		}
		
		if (!Flags.isInterface(flags) && !Flags.isEnum(flags))
			helper.associateDataPropertyToIndividual(OWLSmellDataProperty.clNumberOfAttributes, 
				visitor.getVariables().size(), className);
	}

	private String getEncapsulation(int flag) {
		if (Flags.isPublic(flag))
			return "public";
		else if (Flags.isPrivate(flag))
			return "private";
		else if (Flags.isProtected(flag))
			return "protected";
		else if (Flags.isStatic(flag))
			return "static";
		else if (Flags.isFinal(flag))
			return "final";
		else
			return "none";
	}
	
	private OWLSmellClass getOwlSmellClassByAttributeType(String type) {
		
		// List, ArrayList, Map, HashMap, Set, HashSet, Tree, TreeMap
		// int, String, double, float, char, boolean
		// Integer, Double, Float, Char, Boolean
		if (type.contains("List") || type.contains("Map") || 
				type.contains("Set") || type.contains("Tree") ||
				type.contains("Int") || type.contains("int") || 
				type.contains("Double") || type.contains("double") ||
				type.contains("Char") || type.contains("char") ||
				type.contains("Boolean") || type.contains("boolean") ||
				type.contains("Float") || type.contains("float") ||
				type.contains("String"))
			return OWLSmellClass.JavaType;
		
		return OWLSmellClass.ClassOO;
	}
	
	public void verifyNumberOfSons() {
		List<String> sons = new ArrayList<>(mapOfParents.keySet());
		List<String> parents = new ArrayList<>(mapOfParents.values());
		Map<String, Integer> sonsSize = new HashMap<String, Integer>();
		int c = 0;
		
//		System.out.println(map.toString());
		
		for (String parent : parents) {
			if (sonsSize.containsKey(parent)) {
				c = sonsSize.get(parent);
				c++;
				sonsSize.put(parent, c);
			} else {
				sonsSize.put(parent, 1);
			}
			c = 0;
		}
		
		for (String son : sons) {
			if (!sonsSize.containsKey(son))
				sonsSize.put(son, 0);
		}
		
//		System.out.println(sonsSize.toString());
		
		for (String clazz : sonsSize.keySet()) {
			String value = sonsSize.get(clazz).toString();
			if (!clazz.equalsIgnoreCase("Object")) {
				if (clazz.charAt(clazz.length()-1) == 'I') {
					clazz = clazz.substring(0, clazz.length()-1);
					helper.associateDataPropertyToIndividual(OWLSmellDataProperty.inNumberOfSons, Integer.parseInt(value), clazz);
				} else 
					helper.associateDataPropertyToIndividual(OWLSmellDataProperty.clNumberOfSons, Integer.parseInt(value), clazz);
			}
		}
	}
	
	private String getParameterType(String type) {
		if (type.length() == 1) {
			if (type.equals("I"))
				return "int";
			else if (type.equals("C"))
				return "char";
			else if (type.equals("F"))
				return "float";
			else if (type.equals("D"))
				return "double";
			else if (type.equals("B"))
				return "byte";
			else if (type.equals("S"))
				return "short";
			else if (type.equals("J"))
				return "long";
			else if (type.equals("Z"))
				return "boolean";
			else
				return "unknown";
		} else {
			if (!type.contains("[") && !type.contains("]") && !type.contains("<") && !type.contains(">")) {
				return type.substring(1, type.length()-1);
			} else if (type.startsWith("[[")) {
				return type.substring(3, type.length()-1) + "[][]";
			} else if (type.startsWith("[")) {
				return type.substring(2, type.length()-1) + "[]";
			} else if (type.contains("<") && type.contains(">")) {
				String var = type.substring(1, type.length()-1);
				String var2 = var.substring(var.indexOf("<")+1, var.indexOf(">"));
				String repl = var2.substring(1, var2.length()-1);
				return var.replace(var2, repl);
			} else
				return "unknown";
		}
		
	}

	/**
	 * Reads a ICompilationUnit and creates the AST DOM for manipulating the Java
	 * source file
	 *
	 * @param unit
	 * @return
	 */

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS10);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
}
