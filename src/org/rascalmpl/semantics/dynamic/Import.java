/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:

 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Tijs van der Storm - Tijs.van.der.Storm@cwi.nl
 *   * Mark Hills - Mark.Hills@cwi.nl (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
*******************************************************************************/
package org.rascalmpl.semantics.dynamic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.ast.ImportedModule;
import org.rascalmpl.ast.LocationLiteral;
import org.rascalmpl.ast.Module;
import org.rascalmpl.ast.Name;
import org.rascalmpl.ast.QualifiedName;
import org.rascalmpl.ast.SyntaxDefinition;
import org.rascalmpl.ast.Tag;
import org.rascalmpl.ast.TagString.Lexical;
import org.rascalmpl.interpreter.IEvaluator;
import org.rascalmpl.interpreter.asserts.ImplementationError;
import org.rascalmpl.interpreter.control_exceptions.Throw;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.env.GlobalEnvironment;
import org.rascalmpl.interpreter.env.ModuleEnvironment;
import org.rascalmpl.interpreter.result.ICallableValue;
import org.rascalmpl.interpreter.result.Result;
import org.rascalmpl.interpreter.result.ResultFactory;
import org.rascalmpl.interpreter.result.SourceLocationResult;
import org.rascalmpl.interpreter.staticErrors.ModuleImport;
import org.rascalmpl.interpreter.staticErrors.ModuleNameMismatch;
import org.rascalmpl.interpreter.staticErrors.StaticError;
import org.rascalmpl.interpreter.staticErrors.SyntaxError;
import org.rascalmpl.interpreter.staticErrors.UndeclaredModule;
import org.rascalmpl.interpreter.staticErrors.UndeclaredModuleProvider;
import org.rascalmpl.interpreter.utils.Modules;
import org.rascalmpl.interpreter.utils.Names;
import org.rascalmpl.interpreter.utils.RuntimeExceptionFactory;
import org.rascalmpl.library.lang.rascal.syntax.RascalParser;
import org.rascalmpl.parser.ASTBuilder;
import org.rascalmpl.parser.Parser;
import org.rascalmpl.parser.ParserGenerator;
import org.rascalmpl.parser.gtd.IGTD;
import org.rascalmpl.parser.gtd.exception.ParseError;
import org.rascalmpl.parser.gtd.exception.UndeclaredNonTerminalException;
import org.rascalmpl.parser.gtd.result.action.IActionExecutor;
import org.rascalmpl.parser.gtd.result.out.DefaultNodeFlattener;
import org.rascalmpl.parser.uptr.UPTRNodeFactory;
import org.rascalmpl.parser.uptr.action.NoActionExecutor;
import org.rascalmpl.uri.URIResolverRegistry;
import org.rascalmpl.uri.URIUtil;
import org.rascalmpl.values.ValueFactoryFactory;
import org.rascalmpl.values.uptr.ITree;
import org.rascalmpl.values.uptr.ProductionAdapter;
import org.rascalmpl.values.uptr.RascalValueFactory;
import org.rascalmpl.values.uptr.SymbolAdapter;
import org.rascalmpl.values.uptr.TreeAdapter;
import org.rascalmpl.values.uptr.visitors.IdentityTreeVisitor;

public abstract class Import {
	
	static public class External extends org.rascalmpl.ast.Import.External {

		public External(ISourceLocation src, IConstructor node, QualifiedName name,
				LocationLiteral at) {
			super(src, node, name, at);
		}
		
	
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> eval) {
			// Compute the URI location, which contains the scheme (and other info we need later)
			ISourceLocation sl = (ISourceLocation)getAt().interpret(eval).getValue();
			
			// If we have a resource scheme, given as resource scheme + standard scheme, we
			// extract that out, e.g., jdbctable+mysql would give a resource scheme of
			// jdbctable and a standard URI scheme of mysql. If we do not have a separate
			// resource scheme, we just use the specified scheme, e.g., sdf would give
			// a resource scheme of sdf and a URI scheme of sdf.
			String resourceScheme = sl.getScheme();
			
			if (resourceScheme.contains("+")) {
				String uriScheme = resourceScheme.substring(resourceScheme.indexOf("+")+1); 
				resourceScheme = resourceScheme.substring(0,resourceScheme.indexOf("+"));
				try {
					sl = URIUtil.changeScheme(sl, uriScheme);
				} catch (URISyntaxException e) {
					throw RuntimeExceptionFactory.malformedURI(sl.toString().substring(sl.toString().indexOf("+")+1), null, null);
				}
			}
			
			String moduleName = Names.fullName(this.getName());
			IString mn = VF.string(moduleName);
			
			// Using the scheme, get back the correct importer
			ICallableValue importer = getImporter(resourceScheme, eval.getCurrentEnvt());
			
			if (importer != null) {
				Type[] argTypes = new org.eclipse.imp.pdb.facts.type.Type[] {TF.stringType(), TF.sourceLocationType()};
				IValue[] argValues = new IValue[] { mn, sl };
				
				// Invoke the importer, which should generate the text of the module that we need
				// to actually import.
				IValue module = importer.call(argTypes, argValues, null).getValue();
				String moduleText = module.getType().isString() ? ((IString) module).getValue() : TreeAdapter.yield((IConstructor) module);
				
				moduleText = "@generated\n" + moduleText;
				
				try {
					URIResolverRegistry reg = URIResolverRegistry.getInstance();
					String moduleEnvName = eval.getCurrentModuleEnvironment().getName();
					ISourceLocation ur = null;
					if (moduleEnvName.equals(ModuleEnvironment.SHELL_MODULE)) {
						ur = URIUtil.rootLocation("cwd");
					} else {
						ur = eval.getRascalResolver().getRootForModule(moduleEnvName);
					}
					Result<?> loc = new SourceLocationResult(TF.sourceLocationType(), ur, eval);
					String modulePath = moduleName.replaceAll("::", "/");
					loc = loc.add(ResultFactory.makeResult(TF.stringType(), VF.string(modulePath), eval));
					loc = loc.fieldUpdate("extension", ResultFactory.makeResult(TF.stringType(), VF.string(".rsc"), eval), eval.getCurrentEnvt().getStore());
					
					OutputStream outputStream;
					try {
						outputStream = reg.getOutputStream(((ISourceLocation) loc.getValue()), false);
					}
					catch (IOException e) {
						outputStream = reg.getOutputStream(URIUtil.rootLocation("cwd"), false);
					}
					
					if (outputStream == null) {
						outputStream = reg.getOutputStream(URIUtil.rootLocation("cwd"), false);
					}
					
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
					writer.write(moduleText);
					writer.close();
				}
				catch (IOException e) {
					throw RuntimeExceptionFactory.moduleNotFound(mn, eval.getCurrentAST(), eval.getStackTrace());
				}
				
				importModule(Names.fullName(this.getName()), getLocation(), eval);
				return ResultFactory.nothing();
			} else {
				throw new UndeclaredModuleProvider(resourceScheme, eval.getCurrentAST());
			}
		}
		
		private ICallableValue getImporter(String s, Environment currentEnvt) {
			return currentEnvt.getHeap().getResourceImporter(s);
		}
	}
	
	static public class Extend extends org.rascalmpl.ast.Import.Extend {
		public Extend(ISourceLocation src, IConstructor node, ImportedModule module) {
			super(src, node, module);
		}
		
		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> eval) {
			String name = Names.fullName(this.getModule().getName());
			extendCurrentModule(this.getLocation(), name, eval);
			return org.rascalmpl.interpreter.result.ResultFactory.nothing();
		}
	}

	static public class Default extends org.rascalmpl.ast.Import.Default {
		public Default(ISourceLocation __param1, IConstructor tree, ImportedModule __param2) {
			super(__param1, tree, __param2);
		}

		@Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> eval) {
		  try {
		    importModule(Names.fullName(getModule().getName()), getLocation(), eval);
		  }
		  finally {
		    eval.setCurrentAST(this);
		  }
		  
		  return ResultFactory.nothing();
		}
	}

	static public class Syntax extends org.rascalmpl.ast.Import.Syntax {
		public Syntax(ISourceLocation __param1, IConstructor tree, SyntaxDefinition __param2) {
			super(__param1, tree, __param2);
		}

    @Override
		public Result<IValue> interpret(IEvaluator<Result<IValue>> eval) {
			String parseTreeModName = "ParseTree";
			if (!eval.__getHeap().existsModule(parseTreeModName)) {
				loadModule(getLocation(), parseTreeModName, eval);
			}
			addImportToCurrentModule(getLocation(), parseTreeModName, eval);

			getSyntax().interpret(eval);
			return nothing();
		}
	}

	public static void importModule(String name, ISourceLocation src, IEvaluator<Result<IValue>> eval) {
		GlobalEnvironment heap = eval.__getHeap();
		
		if (!heap.existsModule(name)) {
			// deal with a fresh module that needs initialization
			heap.addModule(new ModuleEnvironment(name, heap));
			loadModule(src, name, eval);
		} 
		else if (eval.getCurrentEnvt() == eval.__getRootScope()) {
			// in the root scope we treat an import as a "reload"
			heap.resetModule(name);
			loadModule(src, name, eval);
		} 
		
		addImportToCurrentModule(src, name, eval);
		
		if (heap.getModule(name).isDeprecated()) {
			eval.getStdErr().println(src + ":" + name + " is deprecated, " + heap.getModule(name).getDeprecatedMessage());
		}
		
		return;
	}

	public static void extendCurrentModule(ISourceLocation x, String name, IEvaluator<Result<IValue>> eval) {
		GlobalEnvironment heap = eval.__getHeap();
		ModuleEnvironment other = heap.getModule(name);

		try {
			if (other == null) {
				// deal with a fresh module that needs initialization
				heap.addModule(new ModuleEnvironment(name, heap));
				other = loadModule(x, name, eval);
			} 
			else if (eval.getCurrentEnvt() == eval.__getRootScope()) {
				// in the root scope we treat an extend as a "reload"
				heap.resetModule(name);
				other = loadModule(x, name, eval);
			} 

			// now simply extend the current module
			eval.getCurrentModuleEnvironment().extend(other); //heap.getModule(name));
		}
		catch (Throwable e) {
			// extending a module is robust against broken modules
			if (eval.isInterrupted()) {
				throw e;
			}
		}
	}
	
  public static ModuleEnvironment loadModule(ISourceLocation x, String name, IEvaluator<Result<IValue>> eval) {
    GlobalEnvironment heap = eval.getHeap();
    
    ModuleEnvironment env = heap.getModule(name);
    if (env == null) {
      env = new ModuleEnvironment(name, heap);
      heap.addModule(env);
    }
    
    try {
    	ISourceLocation uri = eval.getRascalResolver().resolveModule(name);
    	if (uri == null) {
    		throw new ModuleImport(name, "can not find in search path", x);
    	}
      Module module = buildModule(uri, env, eval);

      if (isDeprecated(module)) {
        eval.getStdErr().println("WARNING: deprecated module " + name + ":" + getDeprecatedMessage(module));
      }
      
      if (module != null) {
        String internalName = org.rascalmpl.semantics.dynamic.Module.getModuleName(module);
        if (!internalName.equals(name)) {
          throw new ModuleNameMismatch(internalName, name, x);
        }
        heap.setModuleURI(name, module.getLocation().getURI());
        
        module.interpret(eval);
        
        return env;
      }
    }
    catch (SyntaxError e) {
    	heap.removeModule(env);
        eval.getEvaluator().warning("Could not load " + name, x);
        throw e;
    }
    catch (StaticError | Throw  e) {
      heap.removeModule(env);
      eval.getEvaluator().warning("Could not load " + name, x);
      throw e;
    } catch (Throwable e) {
      heap.removeModule(env);
      eval.getEvaluator().warning("Could not load " + name, x);
      throw new ModuleImport(name, e.getMessage(), x);
    } 

    heap.removeModule(env);
    throw new ImplementationError("Unexpected error while parsing module " + name + " and building an AST for it ", x);
  }
  
  private static boolean isDeprecated(Module preModule){
    for (Tag tag : preModule.getHeader().getTags().getTags()) {
      if (((Name.Lexical) tag.getName()).getString().equals("deprecated")) {
        return true;
      }
    }
    return false;
  }
  
  private static String getDeprecatedMessage(Module preModule){
    for (Tag tag : preModule.getHeader().getTags().getTags()) {
      if (((Name.Lexical) tag.getName()).getString().equals("deprecated")) {
        String contents = ((Lexical) tag.getContents()).getString();
        return contents.substring(1, contents.length() -1);
      }
    }
    return "";
  }
  
  private static Module buildModule(ISourceLocation uri, ModuleEnvironment env,  IEvaluator<Result<IValue>> eval) throws IOException {
	ITree tree = eval.parseModule(eval, uri);
    return getBuilder().buildModule(tree);
  }
  
  private static ASTBuilder getBuilder() {
    return new ASTBuilder();
  }

  private static void addImportToCurrentModule(ISourceLocation src, String name, IEvaluator<Result<IValue>> eval) {
    ModuleEnvironment module = eval.getHeap().getModule(name);
    if (module == null) {
      throw new UndeclaredModule(name, src);
    }
    ModuleEnvironment current = eval.getCurrentModuleEnvironment();
    current.addImport(name, module);
    current.setSyntaxDefined(current.definesSyntax() || module.definesSyntax());
  }
  
  public static ITree parseModule(char[] data, ISourceLocation location, IEvaluator<Result<IValue>> eval){
    eval.__setInterrupt(false);
    IActionExecutor<ITree> actions = new NoActionExecutor();

    try {
      eval.startJob("Parsing " + location, 10);
      eval.event("initial parse");

      ITree tree = new RascalParser().parse(Parser.START_MODULE, location.getURI(), data, actions, new DefaultNodeFlattener<IConstructor, ITree, ISourceLocation>(), new UPTRNodeFactory());
  
      if (TreeAdapter.isAmb(tree)) {
        // Ambiguity is dealt with elsewhere
        return tree;
      }

      ITree top = TreeAdapter.getStartTop(tree);

      String name = Modules.getName(top);

      // create the current module if it does not exist yet
      GlobalEnvironment heap = eval.getHeap();
      ModuleEnvironment env = heap.getModule(name);
      if(env == null){
        env = new ModuleEnvironment(name, heap);
        // do not add the module to the heap here. 
      }
      env.setBootstrap(needBootstrapParser(data));

      // make sure all the imported and extended modules are loaded
      // since they may provide additional syntax definitions\
      Environment old = eval.getCurrentEnvt();
      try {
        eval.setCurrentEnvt(env);
        env.setInitialized(true);

        eval.event("defining syntax");
        eval.getCurrentModuleEnvironment().clearProductions();
        ISet rules = Modules.getSyntax(top);
        for (IValue rule : rules) {
          evalImport(eval, (IConstructor) rule);
        }

        eval.event("importing modules");
        ISet imports = Modules.getImports(top);
        for (IValue mod : imports) {
          evalImport(eval, (IConstructor) mod);
        }

        eval.event("extending modules");
        ISet extend = Modules.getExtends(top);
        for (IValue mod : extend) {
          evalImport(eval, (IConstructor) mod);
        }

        eval.event("generating modules");
        ISet externals = Modules.getExternals(top);
        for (IValue mod : externals) {
          evalImport(eval, (IConstructor) mod);
        }
      }
      finally {
        eval.setCurrentEnvt(old);
      }

      // parse the embedded concrete syntax fragments of the current module
      ITree result = tree;
      if (!eval.getHeap().isBootstrapper() && (needBootstrapParser(data) || (env.definesSyntax() && containsBackTick(data, 0)))) {
        eval.event("parsing concrete syntax");
        result = parseFragments(eval, tree, location, env);
      }

      return result;
    } 
    finally {
      eval.endJob(true);
    }
  }
  
  private static void evalImport(IEvaluator<Result<IValue>> eval, IConstructor mod) {
	  org.rascalmpl.ast.Import imp = (org.rascalmpl.ast.Import) getBuilder().buildValue(mod);
	  try {
		  imp.interpret(eval);
	  }
	  catch (Throwable e) {
		  eval.getEvaluator().warning(e.getMessage(), imp.getLocation());
		  // parsing the current module should be robust wrt errors in modules it depends on.
		  if (eval.isInterrupted()) {
			  throw e;
		  }
	  }
  }
  
  

  /**
   * This function will reconstruct a parse tree of a module, where all nested concrete syntax fragments
   * have been parsed and their original flat literal strings replaced by fully structured parse trees.
   * 
   * @param module is a parse tree of a Rascal module containing flat concrete literals
   * @param parser is the parser to use for the concrete literals
   * @return parse tree of a module with structured concrete literals, or parse errors
   */
  public static ITree parseFragments(final IEvaluator<Result<IValue>> eval, IConstructor module, final ISourceLocation location, final ModuleEnvironment env) {
     return (ITree) module.accept(new IdentityTreeVisitor<ImplementationError>() {
       final IValueFactory vf = eval.getValueFactory();
       
       @Override
       public ITree visitTreeAppl(ITree tree)  {
         IConstructor pattern = getConcretePattern(tree);
         
         if (pattern != null) {
           ITree parsedFragment = parseFragment(eval, env, (ITree) TreeAdapter.getArgs(tree).get(0), location);
           return TreeAdapter.setArgs(tree, vf.list(parsedFragment));
         }
         else {
           IListWriter w = vf.listWriter();
           IList args = TreeAdapter.getArgs(tree);
           for (IValue arg : args) {
             w.append(arg.accept(this));
           }
           args = w.done();
           
           return TreeAdapter.setArgs(tree, args);
         }
       }

       private IConstructor getConcretePattern(ITree tree) {
         String sort = TreeAdapter.getSortName(tree);
         if (sort.equals("Expression") || sort.equals("Pattern") || sort.equals("ExpressionAndPattern")) {
           String cons = TreeAdapter.getConstructorName(tree);
           if (cons.equals("concrete")) {
             return (IConstructor) TreeAdapter.getArgs(tree).get(0);
           }
         }
         return null;
      }

      @Override
       public ITree visitTreeAmb(ITree arg) {
         throw new ImplementationError("unexpected ambiguity: " + arg);
       }
     });
  }
  
  @SuppressWarnings("unchecked")
  public static IGTD<IConstructor, ITree, ISourceLocation> getParser(IEvaluator<Result<IValue>> eval, ModuleEnvironment currentModule, ISourceLocation loc, boolean force) {
    if (currentModule.getBootstrap()) {
      return new RascalParser();
    }
    
    if (currentModule.hasCachedParser()) {
      String className = currentModule.getCachedParser();
      Class<?> clazz;
      for (ClassLoader cl: eval.getClassLoaders()) {
        try {
          clazz = cl.loadClass(className);
          return (IGTD<IConstructor, ITree, ISourceLocation>) clazz.newInstance();
        } catch (ClassNotFoundException e) {
          continue;
        } catch (InstantiationException e) {
          throw new ImplementationError("could not instantiate " + className + " to valid IGTD parser", e);
        } catch (IllegalAccessException e) {
          throw new ImplementationError("not allowed to instantiate " + className + " to valid IGTD parser", e);
        }
      }
      throw new ImplementationError("class for cached parser " + className + " could not be found");
    }

    ParserGenerator pg = eval.getParserGenerator();
    IMap definitions = currentModule.getSyntaxDefinition();
    
    Class<IGTD<IConstructor, ITree, ISourceLocation>> parser = eval.getHeap().getObjectParser(currentModule.getName(), definitions);

    if (parser == null || force) {
      String parserName = currentModule.getName(); // .replaceAll("::", ".");
      parser = pg.getNewParser(eval, loc, parserName, definitions);
      eval.getHeap().storeObjectParser(currentModule.getName(), definitions, parser);
    }

    try {
      return parser.newInstance();
    } catch (InstantiationException e) {
      throw new ImplementationError(e.getMessage(), e);
    } catch (IllegalAccessException e) {
      throw new ImplementationError(e.getMessage(), e);
    } catch (ExceptionInInitializerError e) {
      throw new ImplementationError(e.getMessage(), e);
    }
  }
  
  private static ITree parseFragment(IEvaluator<Result<IValue>> eval, ModuleEnvironment env, ITree tree, ISourceLocation uri) {
    IConstructor symTree = TreeAdapter.getArg(tree, "symbol");
    ITree lit = TreeAdapter.getArg(tree, "parts");
    Map<String, ITree> antiquotes = new HashMap<>();
     
    IGTD<IConstructor, ITree, ISourceLocation> parser = env.getBootstrap() ? new RascalParser() : getParser(eval, env, TreeAdapter.getLocation(tree), false);
    
    try {
      String parserMethodName = eval.getParserGenerator().getParserMethodName(symTree);
      DefaultNodeFlattener<IConstructor, ITree, ISourceLocation> converter = new DefaultNodeFlattener<IConstructor, ITree, ISourceLocation>();
      UPTRNodeFactory nodeFactory = new UPTRNodeFactory();
    
      SortedMap<Integer,Integer> corrections = new TreeMap<>();
      char[] input = replaceAntiQuotesByHoles(eval, lit, antiquotes, corrections);
      
      ITree fragment = (ITree) parser.parse(parserMethodName, uri.getURI(), input, converter, nodeFactory);
      
      // Adjust locations before replacing the holes back to the original anti-quotes,
      // since these anti-quotes already have the right location (!).
      fragment = (ITree) fragment.accept(new AdjustLocations(corrections, eval.getValueFactory()));
      fragment = replaceHolesByAntiQuotes(eval, fragment, antiquotes, corrections);
      
      
      IConstructor prod = TreeAdapter.getProduction(tree);
      IConstructor sym = ProductionAdapter.getDefined(prod);
      sym = SymbolAdapter.delabel(sym); 
      IValueFactory vf = eval.getValueFactory();
      prod = ProductionAdapter.setDefined(prod, vf.constructor(RascalValueFactory.Symbol_Label, vf.string("$parsed"), sym));
      return TreeAdapter.setProduction(TreeAdapter.setArg(tree, "parts", fragment), prod);
    }
    catch (ParseError e) {
      ISourceLocation loc = TreeAdapter.getLocation(tree);
      ISourceLocation src = eval.getValueFactory().sourceLocation(loc.top(), loc.getOffset() + e.getOffset(), loc.getLength(), loc.getBeginLine() + e.getBeginLine() - 1, loc.getEndLine() + e.getEndLine() - 1, loc.getBeginColumn() + e.getBeginColumn(), loc.getBeginColumn() + e.getEndColumn());
      eval.getMonitor().warning("parse error in concrete syntax", src);
      return (ITree) tree.asAnnotatable().setAnnotation("parseError", src);
    }
    catch (StaticError e) {
      ISourceLocation loc = TreeAdapter.getLocation(tree);
      ISourceLocation src = eval.getValueFactory().sourceLocation(loc.top(), loc.getOffset(), loc.getLength(), loc.getBeginLine(), loc.getEndLine(), loc.getBeginColumn(), loc.getBeginColumn());
      eval.getMonitor().warning(e.getMessage(), e.getLocation());
      return (ITree) tree.asAnnotatable().setAnnotation("can not parse fragment due to " + e.getMessage(), src);
    }
    catch (UndeclaredNonTerminalException e) {
      ISourceLocation loc = TreeAdapter.getLocation(tree);
      ISourceLocation src = eval.getValueFactory().sourceLocation(loc.top(), loc.getOffset(), loc.getLength(), loc.getBeginLine(), loc.getEndLine(), loc.getBeginColumn(), loc.getBeginColumn());
      eval.getMonitor().warning(e.getMessage(), src);
      return (ITree) tree.asAnnotatable().setAnnotation("can not parse fragment due to " + e.getMessage(), src);
    }
  }
  
  private static class AdjustLocations extends IdentityTreeVisitor<ImplementationError> {
  	private SortedMap<Integer, Integer> corrections;
  	
		private IValueFactory vf;

		AdjustLocations(SortedMap<Integer, Integer> corrections, IValueFactory vf) {
  		this.corrections = corrections;
  		this.vf = vf;
    }
		
		private int offsetFor(int locOffset) {
			// find the entry k, v in corrections,
			// where k is the largest that is smaller or equal to locOffset.
			if (corrections.isEmpty()) {
				return 0;
			}
			int key = -1;
			SortedMap<Integer, Integer> rest = corrections.tailMap(locOffset);
			if (rest.isEmpty()) {
				key = corrections.lastKey();
				assert key < locOffset;
			}
			else if (rest.firstKey() == locOffset) {
				key = locOffset;
			}
			else {
				assert rest.firstKey() > locOffset;
				
				SortedMap<Integer, Integer> front = corrections.headMap(rest.firstKey());
				if (front.isEmpty()) {
					return 0;
				}
				key = front.lastKey();
				assert key < locOffset;
			}
			int off = corrections.get(key);
			return off;
		}
		
    @Override
    public ITree visitTreeAppl(ITree tree)  {
    	ISourceLocation loc = TreeAdapter.getLocation(tree);
    	if (loc == null) {
    		return tree;
    	}

    	int off = offsetFor(loc.getOffset());
  		loc = vf.sourceLocation(loc, loc.getOffset() + off, loc.getLength());

    	IListWriter w = vf.listWriter();
      IList args = TreeAdapter.getArgs(tree);
      for (IValue arg : args) {
        w.append(arg.accept(this));
      }
      args = w.done();
      
    	return TreeAdapter.setLocation(TreeAdapter.setArgs(tree, args), loc);
    }
    
    @Override
    public ITree visitTreeAmb(ITree arg) throws ImplementationError {
    	return (ITree) TreeAdapter.getAlternatives(arg).iterator().next().accept(this);
    }
  }
  
  private static char[] replaceAntiQuotesByHoles(IEvaluator<Result<IValue>> eval, 
  		ITree lit, Map<String, ITree> antiquotes, SortedMap<Integer, Integer> corrections ) {
    IList parts = TreeAdapter.getArgs(lit);
    StringBuilder b = new StringBuilder();
    
    ISourceLocation loc = TreeAdapter.getLocation(lit);
		int offset = 0;  // where we are in the parse tree
		
		//  012345
		// (Exp)`a \> b` parses as "a > b"
		// this means the loc of > must be shifted right (e.g. + 1)
		// (so we *add* to shift when something bigger becomes smaller)
		int shift = loc.getOffset(); // where we need to be in the location
		corrections.put(offset, shift);
    
    for (IValue elem : parts) {
      ITree part = (ITree) elem;
      String cons = TreeAdapter.getConstructorName(part);
      
      int partLen = TreeAdapter.getLocation(part).getLength();
			if (cons.equals("text")) {
      	offset += partLen;
        b.append(TreeAdapter.yield(part));
      }
      else if (cons.equals("newline")) {
      	shift += partLen - 1;
      	corrections.put(++offset, shift);
        b.append('\n');
      }
      else if (cons.equals("lt")) {
      	corrections.put(++offset, ++shift);
      	b.append('<');
      }
      else if (cons.equals("gt")) {
      	corrections.put(++offset, ++shift);
      	b.append('>');
      }
      else if (cons.equals("bq")) {
      	corrections.put(++offset, ++shift);
      	b.append('`');
      }
      else if (cons.equals("bs")) {
      	corrections.put(++offset, ++shift);
      	b.append('\\');
      }
      else if (cons.equals("hole")) {
        String hole = createHole(eval, part, antiquotes);
        shift += partLen - hole.length();
				offset += hole.length();
				corrections.put(offset, shift);
        b.append(hole);
      }
    }
    
    return b.toString().toCharArray();
  }

  private static String createHole(IEvaluator<Result<IValue>> ctx, ITree part, Map<String, ITree> antiquotes) {
    String ph = ctx.getParserGenerator().createHole(part, antiquotes.size());
    antiquotes.put(ph, part);
    return ph;
  }

  private static ITree replaceHolesByAntiQuotes(final IEvaluator<Result<IValue>> eval, ITree fragment, 
  		final Map<String, ITree> antiquotes, final SortedMap<Integer,Integer> corrections) {
      return (ITree) fragment.accept(new IdentityTreeVisitor<ImplementationError>() {
        private final IValueFactory vf = eval.getValueFactory();
        
        @Override
        public ITree visitTreeAppl(ITree tree)  {
          String cons = TreeAdapter.getConstructorName(tree);
          if (cons == null || !cons.equals("$MetaHole") ) {
            IListWriter w = eval.getValueFactory().listWriter();
            IList args = TreeAdapter.getArgs(tree);
            for (IValue elem : args) {
              w.append(elem.accept(this));
            }
            args = w.done();
            
            return TreeAdapter.setArgs(tree, args);
          }
          
          IConstructor type = retrieveHoleType(tree);
          return  (ITree) antiquotes.get(TreeAdapter.yield(tree)).asAnnotatable().setAnnotation("holeType", type)
          		.asAnnotatable().setAnnotation("category", vf.string("MetaVariable"));
          
        }
        
        private IConstructor retrieveHoleType(ITree tree) {
          IConstructor prod = TreeAdapter.getProduction(tree);
          ISet attrs = ProductionAdapter.getAttributes(prod);

          for (IValue attr : attrs) {
            if (((IConstructor) attr).getConstructorType() == RascalValueFactory.Attr_Tag) {
              IValue arg = ((IConstructor) attr).get(0);
              
              if (arg.getType().isNode() && ((INode) arg).getName().equals("holeType")) {
                return (IConstructor) ((INode) arg).get(0);
              }
            }
          }
          
          throw new ImplementationError("expected to find a holeType, but did not: " + tree);
        }

        @Override
        public ITree visitTreeAmb(ITree arg)  {
          ISetWriter w = vf.setWriter();
          for (IValue elem : TreeAdapter.getAlternatives(arg)) {
            w.insert(elem.accept(this));
          }
          return (ITree) arg.set("alternatives", w.done());
        }
      });
  }
 
  private static boolean containsBackTick(char[] data, int offset) {
    for (int i = data.length - 1; i >= offset; --i) {
      if (data[i] == '`')
        return true;
    }
    return false;
  }
  
  private static boolean needBootstrapParser(char[] input) {
    return new String(input).contains("@bootstrapParser");
  }
}
