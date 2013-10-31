import java.util.regex.*;
import java.io.*;
import java.util.*;

public class DocsMaker {

    static String WIKI_URL = "https://github.com/ut-ras/Rasware2013/wiki",
				  FOLDER_URL = "https://github.com/ut-ras/Rasware2013/tree/master/RASLib/inc",
				  OUTPUT_FILE = "output.txt";

    public static ArrayList<String> readFileLines(File file) throws FileNotFoundException {
        ArrayList<String> lines = new ArrayList<String>();
        Scanner sc = new Scanner(file);
        while(sc.hasNext()) {
            lines.add(sc.nextLine());
        }
        sc.close();
        return lines;
    }
	
	public static String getGithubTagFromFunctionDefinition(String s) {
		return s.trim()
				.replaceAll("[^a-zA-Z0-9 ]", "")
			    .replaceAll("\\s+", "-")
				.toLowerCase();
    }
	
	static Pattern functNamePattern = Pattern.compile("[a-zA-Z0-9]+\\(");
	public static String getFunctName(String functDef) {
		Matcher m = functNamePattern.matcher(functDef);
		m.find();
		String s = m.group();
		return s.substring(0, s.length() - 1); // get rid of open parenthesis
	}

	public static void writeOutput(String filename, String s) throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter(filename), true);
		out.write(s);
		out.close();
	}
	
    static class FunctionDescript {
        private String functDescript, returnDescript;
        private ArrayList<Parameter> parameters;
        private ArrayList<String> notes;

        String functDefinition;

        class Parameter {
            String name, description;
            
            public Parameter(String n, String d) {
                name = n;
                description = d;
            }

            public String toString() {
                return "`" + name + "` " + description;
            }
        }
        
        public FunctionDescript(String functDescript) {
            this.functDescript = functDescript.split(" \\* ")[1];
            parameters = new ArrayList<Parameter>();
            notes = new ArrayList<String>();
        }

        public void addParameter(String line) {
            String s = line.split("@param ")[1];
            int firstSpace = s.indexOf(' ');
            
            parameters.add(new Parameter(
                s.substring(0, firstSpace),
                s.substring(firstSpace+1, s.length())
                ));
        }

        public void addNote(String line) {
            notes.add(line.split("Note: ")[1]);
        }

        public void setReturnDescript(String line) {
            returnDescript = line.split("@return ")[1];
        }

        public void setFunctDefinition(String line) {
            functDefinition = line.trim();
			
			//if (functDefinition.charAt(functDefinition.length() - 1) == ',') {
			//	functDefinition += " .....";
			//}
        }

        public String toString() {
            String paramStr = "",
                   retStr = "",
                   noteStr = "";

            if (parameters.size() > 0) {
                paramStr = "**Parameters:**\n";                

                for (Parameter p: parameters) {
                    paramStr += " * " + p.toString() + "\n";
                }

                paramStr += "\n";
            }
            
            if (returnDescript != null) {
                retStr = "**Returns:**\n * " + returnDescript + "\n\n";
            }

            if (notes.size() > 0) {
                noteStr = "**Notes:**\n";

                for (String n: notes) {
                    noteStr += " * " + n + "\n";
                }

                noteStr += "\n";
            }

            return "###`" + getFunctName(functDefinition) + "`\n\n"
                   + functDescript + "\n\n"
                   + "`" + functDefinition + "`\n\n"
                   + paramStr + retStr + noteStr;
        }
    }

    static class FileDescript {
        private ArrayList<String> includes;
        private ArrayList<FunctionDescript> fdescripts;
        private String filename, githubURL;

        public FileDescript(String fn) {
            this.filename = fn;
            includes = new ArrayList<String>();
            fdescripts = new ArrayList<FunctionDescript>();
        }
    
        public void addFunctDescript(FunctionDescript f) {
            fdescripts.add(f);
        }

        public void addInclude(String line) {
            includes.add(line);
        }

        public String toString() {
            String fdstr = "[Go to the source code for " + filename + 
                "](" + FOLDER_URL + "/" + filename + ")\n\n";

            if (includes.size() > 0) {
                fdstr += "### _Includes_\n\n";

                for (String i: includes) {
                    fdstr += " * `" + i + "`\n";
                }

                fdstr += "\n";
            }

            if (fdescripts.size() > 0) {
                fdstr += "### _Functions_\n\n";
                
                for (FunctionDescript d: fdescripts) {
                    String fname = getFunctName(d.functDefinition);
                
                    fdstr += " * [`" + fname + "`](" + 
						WIKI_URL + "/" + filename + "#"
                        + getGithubTagFromFunctionDefinition(fname) + ")\n";
                }

                fdstr += "\n### _Function Documention_\n\n";
                fdstr += "***\n\n";

                for (FunctionDescript d: fdescripts) {
                    fdstr += d.toString() + "***\n\n";
                }
            }

            return fdstr;
        }

    static Pattern includePattern = Pattern.compile(".*#include.*"),
                   functPattern = Pattern.compile("/\\*\\*.*"),
                   paramPattern = Pattern.compile(".*@param.*"),
                   returnPattern = Pattern.compile(".*@return.*"),
                   notePattern = Pattern.compile(".*Note:.*"),
                   descriptEndPattern = Pattern.compile(" \\*/");

		public void parseLines(ArrayList<String> lines) {
			FunctionDescript curFunct = null;

			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				// if the line matches a #include line, add it to the list of includes
				if (includePattern.matcher(line).matches()) {
					this.addInclude(line);
				}
				// else if the line matches "/**", create a new FunctionDoc object for it
				else if (functPattern.matcher(line).matches()) {
					curFunct = new FunctionDescript(lines.get(i + 1));
				}
				// else if we're currently building a FunctionDoc object, check for 
				else if (curFunct != null) {
					// @param
					if (paramPattern.matcher(line).matches()) {
						curFunct.addParameter(line);
					}
					// @return
					else if (returnPattern.matcher(line).matches()) {
						curFunct.setReturnDescript(line);
					}
					// Note:
					else if (notePattern.matcher(line).matches()) {
						curFunct.addNote(line);
					}
					// End of description:
					else if (descriptEndPattern.matcher(line).matches()) {
                        String s = lines.get(i + 1).trim();
                        
                        while (s.charAt(s.length() - 1) == ',') {
                            s += " " + lines.get(i + 1 + 1).trim();
                            i += 1;
                        }
                    
						curFunct.setFunctDefinition(s);
						this.addFunctDescript(curFunct);
					}
				}
			}
		}
    }

    public static void main(String[] args) throws IOException {
		if (args.length == 0) {
            System.out.println("yo you didn't give us any files to read, dude.");
            return;
        } 
		
		String s = "";
		
        for (int i = 0; i < args.length; i++) {
			try {
				File file = new File(args[i]);
				ArrayList<String> lines = readFileLines(file);
				FileDescript fd = new FileDescript(file.getName());
				fd.parseLines(lines);
				System.out.println(s);
				s += fd.toString();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
        }
            
		writeOutput(OUTPUT_FILE, s);
    }
}
