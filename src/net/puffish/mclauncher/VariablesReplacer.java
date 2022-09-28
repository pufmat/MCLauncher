package net.puffish.mclauncher;

import java.util.Arrays;
import java.util.List;

public class VariablesReplacer {
	public record Variable(String name, String... replacements){}

	private final List<Variable> variables;

	public VariablesReplacer(List<Variable> variables) {
		this.variables = variables;
	}

	public List<String> replace(String str){
		for(Variable variable : variables){
			int index = str.indexOf(variable.name());
			if(index != -1){
				List<String> list = Arrays.asList(variable.replacements());
				list.set(
						0,
						str.substring(0, index) + list.get(0)
				);
				list.set(
						list.size() - 1,
						list.get(list.size() - 1) + str.substring(index + variable.name().length())
				);
				return list;
			}
		}
		return List.of(str);
	}

}
