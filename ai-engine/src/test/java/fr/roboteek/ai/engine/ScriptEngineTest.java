package fr.roboteek.ai.engine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class ScriptEngineTest {
	public static void main(String[] args) throws Exception {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");

        engine.eval("value = 10");
        Boolean greaterThan5 = (Boolean) engine.eval("value > 5");
        Boolean lessThan5 = (Boolean) engine.eval("value < 5");

        System.out.println("10 > 5? " + greaterThan5); // true
        System.out.println("10 < 5? " + lessThan5); // false
        
        engine.eval("test = 'Coucou'");
        Boolean egalite = (Boolean) engine.eval("test == 'Coucou'");
        Boolean nonEgalite = (Boolean) engine.eval("test == 'Essai'");
        System.out.println("'Coucou' == 'Coucou' ? " + egalite);
        System.out.println("'Coucou' == 'Essai' ? " + nonEgalite);
    }
}
