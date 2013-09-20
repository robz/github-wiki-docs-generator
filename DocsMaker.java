import java.util.regex.Pattern;
import java.io.*;
import java.util.*;

public class DocsMaker {

    public static ArrayList<String> readFileLines(String filename) throws FileNotFoundException {
        ArrayList<String> lines = new ArrayList<String>();
        Scanner sc = new Scanner(new File(filename));
        while(sc.hasNext()) {
            lines.add(sc.nextLine());
        }
        sc.close();
        return lines;
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
            functDefinition = line;
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

            return "###`" + functDefinition + "`\n\n" + functDescript + "\n\n"
                   + paramStr + retStr + noteStr;
        }
    }

    static class FileDescript {
        private ArrayList<String> includes;
        private ArrayList<FunctionDescript> fdescripts;
        private String filename, githubURL;

        public FileDescript(String fn, String gh) {
            this.filename = fn;
            this.githubURL = gh;
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
                "](" + githubURL + "/" + filename + ")\n\n";

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
                    fdstr += " * `" + d.functDefinition + "`\n";
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
                    curFunct.setFunctDefinition(lines.get(i + 1));
                    this.addFunctDescript(curFunct);
                }
            }
        }
    }
    }

    public static String getGithubTag(String s) {
        return s.replaceAll(
                "[^a-zA-Z ]", "").replaceAll(
                "\\s+", "-").toLowerCase();
    }

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length == 0) {
            System.out.println("yo you didn't give us any files to read, dude.");
            return;
        } else if (args.length == 1) {
            System.out.println("yo the last argument should be a url of the folder where these files are gonna be on github");
            return;
        }

        String url = args[args.length - 1];

        for (int i = 0; i < args.length - 1; i++) {
            String filename = args[i];
            ArrayList<String> lines = readFileLines(filename);
            FileDescript fd = new FileDescript(filename, url);
            fd.parseLines(lines);
            System.out.println(fd);
        }
    }
}
